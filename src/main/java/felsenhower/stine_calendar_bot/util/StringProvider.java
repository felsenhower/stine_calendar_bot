package felsenhower.stine_calendar_bot.util;

import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

/**
 * This class provides string literals from various *.properties files.
 */
public class StringProvider {

    private static final String BUNDLE_LOCATION = "felsenhower.stine_calendar_bot";
    // Maybe I should put this constant in its own file someday
    private static final String[] BUNDLE_NAMES = { "XPath", "Regex", "Exceptions", "Web", "Directories",
            "CallLevel", "Messages" };
    private final HashMap<String, String> strings;
    private final String prefix;

    public StringProvider(Locale locale) {
        this.prefix = "";
        this.strings = new HashMap<String, String>();

        for (String bundle_name : BUNDLE_NAMES) {

            final ResourceBundle bundle_root = ResourceBundle.getBundle(BUNDLE_LOCATION + "." + bundle_name,
                    new ResourceBundle.Control() {
                        @Override
                        public List<Locale> getCandidateLocales(String name, Locale locale) {
                            return Collections.singletonList(Locale.ROOT);
                        }
                    });

            ResourceBundle bundle_de;
            try {
                if (locale.equals(Locale.GERMAN)) {
                    bundle_de = ResourceBundle.getBundle(BUNDLE_LOCATION + "." + bundle_name, locale);
                } else {
                    throw new Exception();
                }
            } catch (Exception e) {
                bundle_de = null;
            }

            for (Enumeration<String> e = bundle_root.getKeys(); e.hasMoreElements();) {
                String entry = e.nextElement();
                ResourceBundle selectedBundle;
                if (bundle_de != null && bundle_de.containsKey(entry)) {
                    selectedBundle = bundle_de;
                } else {
                    selectedBundle = bundle_root;
                }
                strings.put(bundle_name + "." + entry, selectedBundle.getString(entry));
            }
        }
    }

    private StringProvider(String prefix, HashMap<String, String> strings) {
        this.prefix = prefix;
        this.strings = strings;
    }

    public StringProvider from(String prefix) {
        if (Arrays.stream(BUNDLE_NAMES).anyMatch(key -> key.equals(prefix))) {
            return new StringProvider(prefix + ".", this.strings);
        } else {
            throw new IllegalArgumentException(this.get("Exceptions.UnknownMessagePrefix"));
        }
    }

    /*
     * The reason for this weird backend function for get is that
     * "Object... args" will also accept an empty argument. If you would simply
     * overload the function, the reduced form without args will therefore call
     * itself. Not overloading would make AutoComplete always expand to get(key,
     * args) which is quite annoying since we are almost always displaying
     * constant strings. This seems like an ugly workaround but is most likely
     * the most comfortable solution.
     */
    private String _get(String key, Object... args) {
        key = prefix + key;
        try {
            if (strings.containsKey(key)) {
                return String.format(strings.get(key), args);
            } else {
                throw new Exception();
            }
        } catch (Exception e) {
            return "! " + key + (args.length > 0 ? " ( "
                    + Arrays.stream(args).map(arg -> Objects.toString(arg, "null")).collect(Collectors.joining(" , "))
                    + " )" : "") + " !";
        }
    }

    public String get(String key, Object... args) {
        return _get(key, args);
    }

    public String get(String key) {
        return _get(key);
    }

}
