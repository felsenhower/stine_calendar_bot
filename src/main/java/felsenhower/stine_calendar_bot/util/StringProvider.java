package felsenhower.stine_calendar_bot.util;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

/**
 * This class provides string literals from various *.properties files located
 * at the ClassPath.
 */
public class StringProvider {

    private static final String BUNDLES_LOCATION = "felsenhower.stine_calendar_bot";
    private static final String INDEX_FILE = "index";

    private final HashMap<String, String> strings;
    private final String prefix;
    private final HashSet<String> bundles;
    private final boolean prefixIsDirectory;

    /**
     * Primitive backend constructor
     */
    private StringProvider(String prefix, HashMap<String, String> strings, boolean prefixIsDirectory,
            HashSet<String> bundles) {
        if (!isResourceFolder(BUNDLES_LOCATION)) {
            throw new NullPointerException("ResourceBundles unavailable");
        }
        this.prefix = prefix;
        this.strings = strings;
        this.prefixIsDirectory = prefixIsDirectory;
        this.bundles = bundles;
    }

    /**
     * Creates a new StringProvider
     * 
     * @param locale
     *            the Localisation for the Strings. You can enter all valid
     *            locales here, but anything but Locale.GERMAN will just
     *            redirect to Locale.ENGLISH.
     */
    public StringProvider(Locale locale) {
        this("", new HashMap<String, String>(), false, new HashSet<String>(
                Collections.list(ResourceBundle.getBundle(BUNDLES_LOCATION + "." + INDEX_FILE).getKeys())));

        for (String bundle_name : bundles) {
            final ResourceBundle bundle_root = ResourceBundle.getBundle(BUNDLES_LOCATION + "." + bundle_name,
                    new ResourceBundle.Control() {
                        @Override
                        public List<Locale> getCandidateLocales(String name, Locale locale) {
                            return Collections.singletonList(Locale.ROOT);
                        }
                    });

            ResourceBundle bundle_de;
            try {
                if (locale.equals(Locale.GERMAN)) {
                    bundle_de = ResourceBundle.getBundle(BUNDLES_LOCATION + "." + bundle_name, locale);
                } else {
                    throw new Exception();
                }
            } catch (Exception e) {
                bundle_de = null;
            }

            for (String entry : Collections.list(bundle_root.getKeys())) {
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

    /**
     * Checks if the given identifier is a resource folder on the ClassPath
     * 
     * @param identifier
     *            the directory identifier with dots as seperators
     */
    private static boolean isResourceFolder(String identifier) {
        return (Thread.currentThread().getContextClassLoader().getResource(identifier.replace(".", "/")) != null);
    }

    /**
     * Creates a prefixed StringProvider that will automatically prepend the
     * prefix to all keys with the following properties:
     * 
     * - (new StringProvider()).from("Bundle").get("Key") is equal to (new
     * StringProvider()).get("Bundle.ey")
     * 
     * - (new StringProvider()).from("Folder").from("Bundle") is equal to (new
     * StringProvider()).from("Folder.Bundle")
     * 
     * - You can't apply the get()-methods on a StringProvider whose prefix is
     * not a bundle but a resource folder. You can check this by calling
     * prefixIsDirectory(). Violation will cause an
     * {@link UnsupportedOperationException}
     * 
     * @param prefix
     *            the prefix to prepend
     * 
     * @return the prefixed StringProvider
     */
    public StringProvider from(String prefix) {
        String resultingPrefix = this.prefix + prefix;
        if (this.bundles.contains(resultingPrefix)) {
            return new StringProvider(resultingPrefix + ".", this.strings, false, this.bundles);
        } else if (isResourceFolder(BUNDLES_LOCATION + "." + resultingPrefix)) {
            return new StringProvider(resultingPrefix + ".", this.strings, true, this.bundles);
        } else {
            return null;
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
    private String getString(String key, Object... args) throws UnsupportedOperationException {
        if (this.prefixIsDirectory) {
            throw new UnsupportedOperationException("Can't get strings from directory-prefixed StringProviders");
        }
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

    /**
     * Returns the String that the given key maps to, formatted with the given
     * format specifiers.
     * 
     * @param key
     *            the key of the String with all bundle and directory prefices,
     *            if not otherwise specified by a prefixed StringProvider
     * @param args
     *            the arguments for the format specifiers of the String. A wrong
     *            number of arguments leads to an exceptional result.
     * @return the formatted String. If the key is not found or the number of
     *         arguments is wrong, the key and the arguments will be returned
     *         between two exclamation marks.
     */
    public String get(String key, Object... args) {
        return getString(key, args);
    }

    /**
     * Returns the String that the given key maps without applying any
     * arguments.
     * 
     * @see StringProvider#get(String, Object...)
     */
    public String get(String key) {
        return getString(key);
    }

    /**
     * Determines whether the prefix of this StringProvider is a directory
     * 
     * @return true when the prefix is a directory and false when the prefix is
     *         a bundle.
     */
    public boolean prefixIsDirectory() {
        return this.prefixIsDirectory;
    }

}
