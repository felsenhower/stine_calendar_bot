package io.github.felsenhower.stine_calendar_bot.calendars;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.TreeSet;
import java.util.stream.Collectors;

import io.github.felsenhower.stine_calendar_bot.main.Main;
import io.github.felsenhower.stine_calendar_bot.util.StringProvider;

/**
 * Processes all the downloaed and imported calendars, and merges them into one
 * with help of the {@link Calendar} class.
 */
public class CalendarProcessor {

    private final String calendarData;

    /**
     * Creates a new instance and hence starts the processing.
     * 
     * @param strings
     *            a {@link StringProvider}
     * @param username
     *            the username for STiNE
     * @param password
     *            the password for STiNE
     * @param calendarCache
     *            the directory to cache the calendars in
     * @param echoPages
     *            whether the page contents shall be echo'ed during browsing
     */
    public CalendarProcessor(StringProvider strings, String username, String password, Path calendarCache,
            boolean echoPages) throws IOException {

        // Get all the downloaded calendars
        HashMap<String, String> downloadedCalendars = (new CalendarDataDownloader(strings, username, password,
                echoPages)).getCalendarPool();

        // Get all the calendars from hard drive
        HashMap<String, String> importedCalendars = (new CalendarDataImporter(strings, calendarCache))
                .getCalendarPool();

        // Get the union of all keys (calendar names)
        TreeSet<String> keys = new TreeSet<String>(downloadedCalendars.keySet());
        keys.addAll(importedCalendars.keySet());

        LinkedList<Calendar> calendars = new LinkedList<Calendar>();

        for (String key : keys) {
            if (downloadedCalendars.containsKey(key)) {
                // If we have just downloaded the calendar, we will always save
                // it and by means replace the existing file. There is no easy
                // way to check if the existing file is equivalent to the new
                // one because of timestamps.
                String downloadedCalendarData = downloadedCalendars.get(key);
                calendars.add(new Calendar(downloadedCalendarData, strings));
                System.err.println(strings.get("HumanReadable.Messages.WritingFile", key));
                Main.writeCalendarFile(key, calendarCache, downloadedCalendarData);
            } else {
                // If the calendar is only available from disk, we will take
                // that one as well.
                String importedCalendarData = importedCalendars.get(key);
                calendars.add(new Calendar(importedCalendarData, strings));
            }
        }

        // Finally, steal a header and footer from any calendar (They are very
        // similar if not identical) and merge all the appointments in between.
        this.calendarData = new Calendar(calendars.getLast().getHeader(),
                calendars.stream().map(calendar -> calendar.getBody()).collect(Collectors.joining("\n")),
                calendars.getLast().getFooter(), strings).getCalendarData();
    }

    /**
     * @return the merged calendar data
     */
    public String getCalendarData() {
        return calendarData;
    }

}
