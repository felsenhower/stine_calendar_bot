package io.github.felsenhower.stine_calendar_bot.util;

import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * This class determines the suspected character encoding of a sequence of
 * bytes.
 * 
 * The regular attempt to do this is to heuristically scan for the frequency of
 * common characters that are likely to appear in a document (just like e.g.
 * Mozilla's JUniversalChardet does it). Unfortunately, all these attempts fail
 * quite badly with ICS data (If you try to display the UTF-16 data of STiNE in
 * Firefox, it doesn't recognise the charset correctly at the time I am writing
 * this). So instead, this class searches for predefined substrings inside the
 * text, which requires you to have at least some clue about what the document
 * might look like.
 */
public final class CharsetDetector {

    // @formatter:off
    /**
     * Charsets that are used more often in the WWW, should be looked at
     * first. This doesn't change anything about the functionality, but
     * greatly enhances speed in the average case. In an alphabetically
     * sorted list, UTF-8 would show up quite late, even though it is the
     * most used character encoding.
     * 
     * I based this list on three different statistics:
     * [1] http://trends.builtwith.com/encoding
     * [2] http://www.w3cook.com/charset/
     * [3] http://w3techs.com/technologies/overview/character_encoding/all
     * 
     * As these aren't completely congruent, the order is just based on 
     * the average rank from all three statistics. 
     */
    private final String[] casualCharsets = new String[] {
        "UTF-8",
        "ISO-8859-1",
        "windows-1251",
        "US-ASCII",
        "Shift_JIS",
        "GB2312",
        "windows-1252",
        "EUC-KR",
        "EUC-JP",
        "GBK",
        "UTF-16",
        "UTF-16LE",
        "UTF-16BE",
        "ISO-8859-2",
        "Big5",
        "ISO-8859-15",
        "windows-1250",
        "ISO-8859-9",
        "windows-1254",
        "x-windows-874"
    };
    // @formatter:on

    /**
     * This linked (ordered) unmodifiable HashSet contains all supported
     * charsets in order in which they would be checked: First the default
     * charset, then the sets declared in casualCharsets, then all leftover
     * charsets. Duplicates or unsupported charsets get excluded.
     */
    private final Set<Charset> charsets;

    /**
     * Creates a new CharsetDetector with prioritised charsets
     * 
     * @param priorityCharsets
     *            the Charsets which shall be checked first
     */
    public CharsetDetector(Charset... priorityCharsets) {
        this.charsets = Collections.unmodifiableSet(new LinkedHashSet<Charset>() {
            private static final long serialVersionUID = -5512144535881497182L;
            {
                // Add the prioritised charsets first
                addAll(Arrays.asList(priorityCharsets));
                // Then, add the default charset
                add(Charset.defaultCharset());
                // Then, add the common charsets specified in casualCharsets if
                // they are supported
                for (String representation : casualCharsets) {
                    if (Charset.isSupported(representation)) {
                        Charset charset = Charset.forName(representation);
                        add(charset);
                    }
                }
                // Finally, add all remaining supported charsets
                addAll(Charset.availableCharsets().values());
            }
        });
    }

    /**
     * Creates a new CharsetDetector with prioritised charsets
     * 
     * @param priorityCharsets
     *            the String representations of the Charsets which shall be
     *            checked first
     */
    public CharsetDetector(String... priorityCharsets) {
        // @formatter:off
        this(Arrays.stream(priorityCharsets)
                   .map(string -> Charset.forName(string))
                   .toArray(Charset[]::new));
        // @formatter:on
    }

    /**
     * Creates a new CharsetDetector without prioritised charsets
     */
    public CharsetDetector() {
        this(new Charset[0]);
    }

    /**
     * Finds out the encoding of the given byte array by searching for all given
     * hints inside it. This is a very brute-force approach and requires you to
     * know a little bit how your String should look like. If no Charsets apply,
     * [null,null] will be returned.
     * 
     * @param text
     *            the text to search in
     * @param hints
     *            all Strings that have to be contained in the text
     * @return the suspected Charset and rightfully encoded String
     */
    public Pair<Charset, String> testCharsets(byte[] text, String... hints) {
        for (Charset charset : this.getCharsets()) {
            String probe = new String(text, charset);
            for (String hint : hints) {
                if (!probe.contains(hint)) {
                    break;
                }
                return new Pair<Charset, String>(charset, probe);
            }
        }
        return new Pair<Charset, String>();
    }

    /**
     * Returns the suspected encoding of the text. If you also need the
     * rightfully encoded String, do NOT use
     * {@link CharsetDetector#getStringFromBytes(byte[], String...)} afterwards,
     * but rather {@link CharsetDetector#testCharsets() testCharsets()} instead!
     * 
     * @param text
     *            the text to search in
     * @param hints
     *            all Strings that have to be contained in the text
     * @return the suspected charset
     */
    public Charset getCharsetFromBytes(byte[] text, String... hints) {
        return this.testCharsets(text, hints).getKey();
    }

    /**
     * Returns the rightfully encoded String from the text. If you also need the
     * suspected encoding, do NOT use
     * {@link CharsetDetector#getCharsetFromBytes(byte[], String...)} too, but
     * rather {@link CharsetDetector#testCharsets() testCharsets()} instead!
     * 
     * @param text
     *            the text to search in
     * @param hints
     *            all Strings that have to be contained in the text
     * @return the rightfully encoded text
     */
    public String getStringFromBytes(byte[] text, String... hints) {
        return this.testCharsets(text, hints).getValue();
    }

    /**
     * Returns the linked Set of all supported charsets in the order in which
     * they will be checked.
     */
    public Set<Charset> getCharsets() {
        return this.charsets;
    }
}
