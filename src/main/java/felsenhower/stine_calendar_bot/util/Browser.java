package felsenhower.stine_calendar_bot.util;

import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Level;
import java.util.stream.Collectors;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.DomElement;
import com.gargoylesoftware.htmlunit.html.DomNode;
import com.gargoylesoftware.htmlunit.html.HtmlDivision;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

/**
 * This class is used for browsing through the STiNE website. It mainly is a
 * wrapper for HTMLUnit's WebClient class, but also adds a few convenience
 * methods that make browsing a lot less wordy.
 */
public class Browser {

    private final WebClient webclient;

    private HtmlPage page = null;
    
    private final boolean echoPages;

    public Browser(String startpage, boolean echoPages) throws IOException {

        // Turn off annoying HTMLUnit logging
        java.util.logging.Logger.getLogger("com.gargoylesoftware").setLevel(Level.OFF);

        webclient = new WebClient(BrowserVersion.BEST_SUPPORTED);

        // Initialise browser settings
        webclient.getOptions().setJavaScriptEnabled(true);
        webclient.getOptions().setUseInsecureSSL(true);
        webclient.getCookieManager().setCookiesEnabled(true);

        this.gotoURL(startpage);
        
        this.echoPages = echoPages;
    }

    /**
     * @return the current page the Browser is on
     */
    public HtmlPage getPage() {
        return this.page;
    }

    /**
     * Applies the given page to the Browser.
     * 
     * @param page
     *            the Page to browse to.
     * @param showContent
     *            whether the page shall be echo'd to stdout
     */
    public void setPage(Page page, boolean showContent) {
        this.page = (HtmlPage) page;
        if (showContent) {
            showPageContent();
        }
    }

    /**
     * Applies the given page to the Browser. Doesn't echo page content.
     * 
     * @see Browser#setPage(Page, boolean)
     */
    public void setPage(Page page) {
        setPage(page, echoPages);
    }

    /**
     * Navigates the Browser to the given URL.
     * 
     * @param url
     *            the URL to navigate to.
     * @param showContent
     *            whether the page shall be echo'd to stdout
     */
    public void gotoURL(URL url, boolean showContent) throws IOException {
        this.setPage(webclient.getPage(url), showContent);
    }

    /**
     * Navigates the Browser to the given URL.
     * 
     * @see Browser#gotoURL(URL, boolean)
     */
    public void gotoURL(String url, boolean showContent) throws IOException {
        this.setPage(webclient.getPage(url), showContent);
    }

    /**
     * Navigates the Browser to the given URL. Doesn't echo page content
     * 
     * @see Browser#gotoURL(URL, boolean)
     */
    public void gotoURL(URL url) throws IOException {
        this.gotoURL(url, false);
    }

    /**
     * Navigates the Browser to the given URL. Doesn't echo page content
     * 
     * @see Browser#gotoURL(URL, boolean)
     */
    public void gotoURL(String url) throws IOException {
        this.gotoURL(url, false);
    }

    /**
     * Echo's the content of the current page to stdout. All empty lines will be
     * removed and separator containing of dashed will be inserted before and
     * after. This abuses the fact that all STiNE pages use a div element with
     * the id pageContent.
     */
    public void showPageContent() {
        DomNode context = (DomNode) this.page.getElementById("pageContent");
        if (context == null) {
            context = this.page;
        }

        // This removes all the empty lines from the page's content and acquires
        // the result.
        String pageContent = Arrays.stream(context.asText().replaceAll("\\r\\n?", "\n").split("\n"))
                .filter(line -> !line.matches("\\p{Z}*")).collect(Collectors.joining("\n"));

        // This creates a 50 chars long sequence of dashes
        String horzLine = new String(new char[50]).replace("\0", "-");

        System.out.println(horzLine);
        System.out.println(pageContent);
        System.out.println(horzLine);
    }

    /**
     * Gets a list of elements by the given XPath Query on the given context.
     * 
     * @see DomNode#getByXPath(String)
     */
    public static List<?> getByXPath(String xPath, DomNode context) {
        return context.getByXPath(xPath);
    }

    /**
     * Gets a list of elements by the given XPath Query on the current page.
     * 
     * @see Browser#getByXPath(String, DomNode)
     */
    public List<?> getByXPath(String xPath) {
        return getByXPath(xPath, page);
    }

    /**
     * Gets the first element by the given XPath Query on the given context.
     * 
     * @see DomNode#getFirstByXPath(String)
     */
    public static DomElement getFirstByXPath(String xPath, DomNode context) {
        return context.getFirstByXPath(xPath);
    }

    /**
     * Gets the first element by the given XPath Query on the given context.
     * 
     * @see Browser#getFirstByXPath(String, DomNode)
     */
    public DomElement getFirstByXPath(String xPath) {
        return getFirstByXPath(xPath, page);
    }

    /**
     * Clicks on the first element that matches the given XPath Query on the
     * given context.
     * 
     * @param ignoreOnAbsence
     *            whether we shall not throw a NullPointerException if the
     *            element does not exist.
     * 
     * @see Browser#getFirstByXPath(String, DomNode)
     */
    public void clickOnElementByXPath(String xPath, DomNode context, boolean ignoreOnAbsence) throws IOException {
        DomElement clickable = (DomElement) getFirstByXPath(xPath, context);

        if (clickable != null || !ignoreOnAbsence) {
            setPage(clickable.click());
        }
    }

    /**
     * Clicks on the first element that matches the given XPath Query on the
     * given context. Continues on the absent of the target.
     * 
     * @see Browser#clickOnElementByXPath(String, DomNode, boolean)
     */
    public void clickOnElementByXPath(String xPath, DomNode context) throws IOException {
        clickOnElementByXPath(xPath, context, true);
    }

    /**
     * Clicks on the first element that matches the given XPath Query on the
     * current page.
     * 
     * @see Browser#clickOnElementByXPath(String, DomNode, boolean)
     */
    public void clickOnElementByXPath(String xPath, boolean ignoreOnAbsence) throws IOException {
        clickOnElementByXPath(xPath, page, ignoreOnAbsence);
    }

    /**
     * Clicks on the first element that matches the given XPath Query on the
     * current page. Continues on the absent of the target.
     * 
     * @see Browser#clickOnElementByXPath(String, DomNode, boolean)
     */
    public void clickOnElementByXPath(String xPath) throws IOException {
        clickOnElementByXPath(xPath, true);
    }

    /**
     * Refreshes the current page
     * 
     * @param hard
     *            specifies that we shall acquire the URL and go there (hard
     *            refresh) or simply use {@link HtmlPage#refresh()}.
     * @param clearcache
     *            whether the cache should be emptied.
     */
    public void refresh(boolean hard, boolean clearcache) throws IOException {
        if (hard) {
            if (clearcache) {
                webclient.getCache().clear();
            }
            this.gotoURL(page.getBaseURL());
        } else {
            setPage(page.refresh());
        }
    }

}
