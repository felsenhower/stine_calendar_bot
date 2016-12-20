package io.github.felsenhower.stine_calendar_bot.calendars;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;

import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.html.HtmlAnchor;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlOption;
import com.gargoylesoftware.htmlunit.html.HtmlPasswordInput;
import com.gargoylesoftware.htmlunit.html.HtmlSelect;
import com.gargoylesoftware.htmlunit.html.HtmlTextInput;

import io.github.felsenhower.stine_calendar_bot.util.Browser;
import io.github.felsenhower.stine_calendar_bot.util.CharsetDetector;
import io.github.felsenhower.stine_calendar_bot.util.StringProvider;

/**
 * This class extends {@link CalendarDataSupplier} and supplies the HashMap of
 * calendar data Strings that it downloads from the STiNE website.
 */
public class CalendarDataDownloader extends CalendarDataSupplier {

    private final Browser browser;

    /**
     * Creates a new instance of CalendarDataDownloader
     * 
     * @param strings
     *            a {@link StringProvider}
     * @param username
     *            the username for STiNE
     * @param password
     *            the password for STiNE
     * @param echoPages
     *            whether the pages shall be echo'ed during browsing.
     * @throws IOException
     *             can only be thrown during browsing to the download page. Once
     *             we are there, every Exception will be ignored.
     */
    public CalendarDataDownloader(StringProvider strings, String username, String password, boolean echoPages)
            throws IOException {

        StringProvider messages = strings.from("HumanReadable.Messages");
        StringProvider xpath = strings.from("MachineReadable.XPath");

        System.err.println(messages.get("LoadingMainPage"));
        browser = new Browser(strings.get("MachineReadable.Web.Startpage"), echoPages);

        // Get the login form and enter credentials
        final HtmlForm form = (HtmlForm) browser.getFirstByXPath(xpath.get("LoginForm"));
        final HtmlTextInput userfield = (HtmlTextInput) Browser.getFirstByXPath(xpath.get("LoginFormUserField"), form);
        final HtmlPasswordInput passfield = (HtmlPasswordInput) Browser.getFirstByXPath(xpath.get("LoginFormPassField"),
                form);
        userfield.setValueAttribute(username);
        passfield.setValueAttribute(password);
        System.err.println(messages.get("LoggingIn"));
        browser.clickOnElementByXPath(xpath.get("LoginFormSubmitButton"), form);

        // Ensure that the correct language is being used.
        System.err.println(messages.get("SetLang"));
        browser.clickOnElementByXPath(xpath.get("LangSwitchAnchor"));

        // Go to the calendar export page
        System.err.println(messages.get("PreparingDownload"));
        browser.clickOnElementByXPath(xpath.get("SchedulerAnchor"));
        browser.clickOnElementByXPath(xpath.get("SchedulerExportAnchor"));

        // Get the drop-down box for months and acquire all possible values
        HtmlSelect select = (HtmlSelect) browser.getFirstByXPath(xpath.get("MonthSelect"));

        // Get the Select's list of options.
        @SuppressWarnings("unchecked")
        final List<HtmlOption> options = (List<HtmlOption>) Browser.getByXPath(xpath.get("MonthSelectOptions"), select);

        URL downloadPageURL = null;

        for (HtmlOption option : options) {
            try {

                // This acquires the option's inner HTML (or text attribute)
                // which will be in the format like Y2017M01 for January 2017.
                // Take a look in
                // src/main/resources/felsenhower/stine_calendar_bot/XPath.properties
                // for more information. We use this as a identifier and
                // filename for the calendars as they are quite easy to read and
                // automatically sort.
                String name = option.getValueAttribute();

                // Before each new download, we will have to go to the download
                // page first.
                if (downloadPageURL == null) {
                    downloadPageURL = browser.getPage().getBaseURL();
                } else {
                    browser.gotoURL(downloadPageURL);
                }

                // The select is somewhat bound to the Page instance which we
                // will have discarded before. So we need to re-acquire it for
                // the current page. This time, we select the current option as
                // well. This conveniently works by the option's name
                System.err.println(messages.get("Exporting", name));
                select = (HtmlSelect) browser.getFirstByXPath(xpath.get("MonthSelect"));
                select.setSelectedAttribute(name, true);

                // Click the export button and finally get the anchor for
                // downloading the calendar. That anchor may be null if the
                // selected calendar month is empty or the anchor is otherwise
                // absent.
                browser.clickOnElementByXPath(xpath.get("ExportButton"));
                HtmlAnchor downloadLink = (HtmlAnchor) browser.getFirstByXPath(xpath.get("DownloadAnchor"));

                if (downloadLink != null) {
                    System.err.println(messages.get("Downloading"));
                    Page response = downloadLink.click();
                    // First, get the file as a Stream and directly convert that
                    // to byte[]
                    byte[] calendarDataAsBytes = IOUtils.toByteArray(response.getWebResponse().getContentAsStream());

                    // Then, use the CharsetDetector to find out the encoding.
                    // This is a brute-force approach which is testing all
                    // available charsets until the two given Strings are
                    // contained in the result. This is needed because all
                    // common charset guessers have failed so far on these ICS
                    // files, but at least we know how the file should look
                    // like. Because STiNE is usually using UTF16-LE, we are
                    // checking that first which will make the brute-force
                    // rather be a magic guess-right-on-first-try.
                    String calendarData = new CharsetDetector("UTF-16LE").getStringFromBytes(calendarDataAsBytes,
                            "BEGIN:VCALENDAR", "END:VCALENDAR");

                    // Check if the calendarData is well-formed.
                    if (Pattern.compile(strings.get("MachineReadable.Regex.WellFormedIcsData"), Pattern.DOTALL)
                            .matcher(calendarData).matches()) {
                        // Put the calendarData into result HashMap, with the
                        // name as the key.
                        this.calendarPool.put(name, calendarData);
                    } else {
                        System.err.println(messages.get("CalendarIsInvalid"));
                    }
                } else {
                    System.err.println(messages.get("CalendarIsEmpty"));
                }
            } catch (Exception e) {
                // ignore
            }
        }
    }
}
