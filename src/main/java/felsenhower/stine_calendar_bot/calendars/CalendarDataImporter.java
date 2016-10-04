package felsenhower.stine_calendar_bot.calendars;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;

import org.apache.commons.io.FilenameUtils;

import felsenhower.stine_calendar_bot.util.StringProvider;

/**
 * This class extends {@link CalendarDataSupplier} and supplies the HashMap of
 * calendar data Strings that it imports from the calendar cache on the hard
 * disk.
 */
public class CalendarDataImporter extends CalendarDataSupplier {

    /**
     * Creates a new instance of CalendarDataDownloader
     * 
     * @param strings
     *            a {@link StringProvider}
     * @throws IOException
     *             when anything unexpected happens during the file operations
     */
    public CalendarDataImporter(StringProvider strings, Path calendarCache) throws IOException {
        File calendarCacheFile = calendarCache.toFile();
        if (calendarCacheFile.exists()) {
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(calendarCache)) {
                for (Path entry : stream) {
                    String name = FilenameUtils.removeExtension(entry.getFileName().toString());
                    System.err.println(strings.get("Messages.LoadingLocalFile", name));
                    String calendarData = new String(Files.readAllBytes(entry));
                    if (Pattern.compile(strings.get("Regex.WellFormedIcsData"), Pattern.DOTALL).matcher(calendarData)
                            .matches()) {
                        this.calendarPool.put(name, calendarData);
                    } else {
                        System.err.println(strings.get("Messages.CalendarIsInvalid", name));
                    }
                }
            }
        } else {
            calendarCacheFile.mkdirs();
        }
    }
}
