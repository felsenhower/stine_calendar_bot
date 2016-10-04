package felsenhower.stine_calendar_bot.calendars;

import java.util.HashMap;

/**
 * This abstract class describes a basic supplier for calendar data. The
 * calendar data will be collected in a HashMap, see {@link getCalendarPool()}.
 * For classes that extend this, see {@link CalendarDataImporter} and
 * {@link CalendarDataDownloader}.
 */
public abstract class CalendarDataSupplier {

    protected final HashMap<String, String> calendarPool = new HashMap<String, String>();

    /**
     * Returns a HashMap of String to String. The keys shall be in a format like
     * Y2017M01 for January 2017. The strings shall always be valid calendar
     * data in UTF-8.
     * 
     * @return the calendars
     */
    public HashMap<String, String> getCalendarPool() {
        return calendarPool;
    }

}
