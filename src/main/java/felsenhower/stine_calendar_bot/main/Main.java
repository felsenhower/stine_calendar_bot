package felsenhower.stine_calendar_bot.main;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;

import org.apache.commons.io.FileUtils;

import felsenhower.stine_calendar_bot.calendars.CalendarProcessor;
import felsenhower.stine_calendar_bot.util.StringProvider;

//TODO: Add better documentation here
/**
 * Stine Calendar Bot
 */
public class Main {
    private final StringProvider strings;

    public static void main(String[] args) {
        try {
            new Main(args);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Main(String[] args) throws Exception {
        
        CallLevelWrapper CLI = new CallLevelWrapper(args);
        
        this.strings = CLI.getStringProvider();

        final String user = CLI.getUser();
        final String pass = CLI.getPass();
        final boolean echoPages = CLI.isEchoPages();
        final Path cache_dir = CLI.getCache_dir();
        final Path output = CLI.getOutput();
        final boolean echoCalendar = CLI.isEchoCalendar();

        String calendarData = (new CalendarProcessor(strings, user, pass, cache_dir, echoPages)).getCalendarData();

        if (echoCalendar) {
            System.out.println(calendarData);
        } else {
            System.err.println(strings.get("HumanReadable.Messages.ExportingFile", output.getFileName()));
            output.toFile().mkdirs();
            Main.writeCalendarFile(output, calendarData);
        }
    }

    private static void writeCalendarFile(Path oldFilePath, Path newFilePath, String content) throws IOException {
        final File oldFile = oldFilePath.toFile();
        final File newFile = newFilePath.toFile();

        FileUtils.writeStringToFile(newFile, content, "UTF-8");

        if (oldFile.exists()) {
            oldFile.delete();
        }
        newFile.renameTo(oldFile);
    }

    public static void writeCalendarFile(Path filename, String content) throws IOException {
        writeCalendarFile(filename, filename.resolveSibling(filename.getFileName() + ".part"), content);
    }

    public static void writeCalendarFile(String name, Path dir, String content) throws IOException {
        final Path oldFilePath = dir.resolve(name + ".ics");
        final Path newFilePath = dir.resolve(name + ".ics.part");
        writeCalendarFile(oldFilePath, newFilePath, content);
    }

}
