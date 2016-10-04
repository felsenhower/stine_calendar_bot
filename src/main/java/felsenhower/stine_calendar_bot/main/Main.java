package felsenhower.stine_calendar_bot.main;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import org.apache.commons.io.FileUtils;

import felsenhower.stine_calendar_bot.calendars.CalendarProcessor;
import felsenhower.stine_calendar_bot.util.StringProvider;

/**
 * STiNE Calendar Bot: An automatic calendar download bot for STiNE.
 * 
 * @author felsenhower
 * 
 */
public class Main {
    private final StringProvider strings;

    /**
     * GLOBAL MAIN METHOD
     */
    public static void main(String[] args) {
        try {
            new Main(args);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Main(String[] args) throws Exception {

        // Do all the console interaction
        CallLevelWrapper CLI = new CallLevelWrapper(args);

        // The ConsoleWrapper actually supplies its own StringProvider which it
        // acquires by parsing the (optional) --language option.
        this.strings = CLI.getStringProvider();

        final String user = CLI.getUser();
        final String pass = CLI.getPass();
        final boolean echoPages = CLI.isEchoPages();
        final Path cache_dir = CLI.getCalendarCache();
        final Path output = CLI.getOutputFilename();
        final boolean echoCalendar = CLI.isEchoCalendar();

        // Acquire the calendar data
        // NOTE: This takes time.
        String calendarData = (new CalendarProcessor(strings, user, pass, cache_dir, echoPages)).getCalendarData();

        // Echo the calendar to stdout or save it to file
        if (echoCalendar) {
            System.out.println(calendarData);
        } else {
            System.err.println(strings.get("HumanReadable.Messages.ExportingFile", output.getFileName()));
            output.toFile().mkdirs();
            Main.writeCalendarFile(output, calendarData);
        }
    }

    /**
     * Writes a calendar to file.
     * 
     * @param oldFilePath
     *            the target filename. If it already exists, it will be
     *            replaced.
     * @param newFilePath
     *            the temporary filename
     * @param content
     *            the calendar data
     */
    private static void writeCalendarFile(Path oldFilePath, Path newFilePath, String content) throws IOException {
        final File oldFile = oldFilePath.toFile();
        final File newFile = newFilePath.toFile();

        FileUtils.writeStringToFile(newFile, content, "UTF-8");

        if (oldFile.exists()) {
            oldFile.delete();
        }
        newFile.renameTo(oldFile);
    }

    /**
     * Writes a calendar to file.
     * 
     * @see Main#writeCalendarFile(String, Path, String)
     */
    public static void writeCalendarFile(Path filename, String content) throws IOException {
        writeCalendarFile(filename, filename.resolveSibling(filename.getFileName() + ".part"), content);
    }

    /**
     * Writes a calendar to file.
     * 
     * @param name
     *            the filename without the .ics extension
     * @param dir
     *            the parent directory
     * @param content
     *            the calendar data
     * 
     * @see Main#writeCalendarFile(String, Path, String)
     */
    public static void writeCalendarFile(String name, Path dir, String content) throws IOException {
        final Path oldFilePath = dir.resolve(name + ".ics");
        final Path newFilePath = dir.resolve(name + ".ics.part");
        writeCalendarFile(oldFilePath, newFilePath, content);
    }

}
