package felsenhower.stine_calendar_bot.calendars;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.TreeSet;
import java.util.stream.Collectors;

import felsenhower.stine_calendar_bot.main.Main;
import felsenhower.stine_calendar_bot.util.StringProvider;

public class CalendarProcessor {

    private final String calendarData;

    public CalendarProcessor(StringProvider strings, String username, String password, Path cache_dir,
            boolean echoPages) throws Exception {

        HashMap<String, String> downloadedCalendars = (new CalendarDataDownloader(strings, username, password,
                echoPages)).getCalendarPool();

        HashMap<String, String> importedCalendars = (new CalendarDataImporter(strings, cache_dir)).getCalendarPool();

        TreeSet<String> keys = new TreeSet<String>(downloadedCalendars.keySet());

        keys.addAll(importedCalendars.keySet());

        LinkedList<Calendar> calendars = new LinkedList<Calendar>();

        for (String key : keys) {
            if (downloadedCalendars.containsKey(key)) {
                String downloadedCalendarData = downloadedCalendars.get(key);
                calendars.add(new Calendar(downloadedCalendarData, strings));
                System.err.println(strings.get("HumanReadable.Messages.OverwritingFile", key));
                Main.writeCalendarFile(key, cache_dir, downloadedCalendarData);
            } else {
                String importedCalendarData = importedCalendars.get(key);
                calendars.add(new Calendar(importedCalendarData, strings));
            }
        }

        this.calendarData = calendars.getLast().getHeader()
                + calendars.stream().map(calendar -> calendar.getBody()).collect(Collectors.joining("\n"))
                + calendars.getLast().getFooter();
    }

    /**
     * @return the calendarData
     */
    public String getCalendarData() {
        return calendarData;
    }

}
