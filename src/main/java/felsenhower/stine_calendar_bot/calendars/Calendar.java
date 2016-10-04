package felsenhower.stine_calendar_bot.calendars;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import felsenhower.stine_calendar_bot.util.StringProvider;

/**
 * Wraps an ICS calendar and splits it up into its primary sections (header,
 * body, and footer), or creates the calendar data from those portions.
 * 
 * @see Calendar#getHeader()
 * @see Calendar#getBody()
 * @see Calendar#getFooter()
 * @see Calendar#getCalendarData()
 */
public class Calendar {

    private final String header;
    private final String footer;
    private final String body;
    private final String calendarData;

    /**
     * Creates a new instance of Calendar
     * 
     * @param calendarData
     *            valid ICS data as a single String with line breaks.
     * @param strings
     *            a {@link StringProvider}
     * @throws IllegalArgumentException
     *             if the calendar data is mal-formed.
     */
    public Calendar(String calendarData, StringProvider strings) throws IllegalArgumentException {
        
        StringProvider messages = strings.from("HumanReadable.Messages");
        StringProvider regex = strings.from("MachineReadable.Regex");

        if (calendarData == null) {
            throw new IllegalArgumentException(messages.get("CalendarDataIsNull"));
        }
        
        this.calendarData = calendarData;

        Matcher calendarDataMatcher = Pattern.compile(regex.get("WellFormedIcsData"), Pattern.DOTALL)
                .matcher(calendarData);
        // Check if the calendar data is valid (and throw an Exception
        // otherwise). Split up the data into the three primary sections
        if (calendarDataMatcher.matches()) {
            this.header = calendarDataMatcher.group(1);
            this.body = calendarDataMatcher.group(2);
            this.footer = calendarDataMatcher.group(3);
        } else {
            throw new IllegalArgumentException(messages.get("CalendarDataIsInvalid"));
        }
    }
    
    /**
     * Creates a new instance of Calendar from header, body, and footer.
     */
    public Calendar(String header, String body, String footer, StringProvider strings) {
        this(header + "\n" + body + "\n" + footer, strings);
    }

    /**
     * @return The header of the calendar (everything between BEGIN:VCALENDAR
     *         and END:VTIMEZONE)
     */
    public String getHeader() {
        return header;
    }

    /**
     * @return The footer of the calendar (probably just END:VCALENDAR)
     */
    public String getFooter() {
        return footer;
    }

    /**
     * @return The body of the calendar (array of VEVENT elements)
     */
    public String getBody() {
        return body;
    }

    /**
     * @return the complete calendar data
     */
    public String getCalendarData() {
        return calendarData;
    }

}
