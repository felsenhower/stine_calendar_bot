package felsenhower.stine_calendar_bot.main;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.MissingArgumentException;
import org.apache.commons.cli.MissingOptionException;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionGroup;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.UnrecognizedOptionException;

import felsenhower.stine_calendar_bot.util.StringProvider;

/**
 * Creates a new instance
 * 
 * @param args
 *            the submitted args from the main method.
 */
public class CallLevelWrapper {

    private final String username;
    private final String password;
    private final boolean echoPages;
    private final Path calendarCache;
    private final Path outputFile;
    private final boolean echoCalendar;

    private final StringProvider strings;
    private final StringProvider cliStrings;
    private final StringProvider messages;
    private final StringProvider appInfo;

    final private Options options;

    public CallLevelWrapper(String[] args) throws IOException {
        String username = null;
        String password = null;
        boolean echoPages = false;
        Path calendarCache = null;
        Path outputFile = null;
        boolean echoCalendar = false;

        // These temporary options don't have descriptions and have their
        // required-value all set to false
        final Options tempOptions = getOptions(null);

        final CommandLineParser parser = new DefaultParser();
        CommandLine cmd = null;
        StringProvider strings = null;

        // Get the StringProvider
        try {
            cmd = parser.parse(tempOptions, args);
            if (cmd.hasOption("language")) {
                String lang = cmd.getOptionValue("language").toLowerCase();
                if (lang.equals("de")) {
                    strings = new StringProvider(Locale.GERMAN);
                } else {
                    strings = new StringProvider(Locale.ENGLISH);
                    if (!lang.equals("en")) {
                        System.err.println(strings.get("HumanReadable.CallLevel.LangNotRecognised", lang));
                    }
                }
            } else {
                strings = new StringProvider(new Locale(Locale.getDefault().getLanguage()));
            }
        } catch (Exception e) {
            strings = new StringProvider(Locale.ENGLISH);
        }

        this.strings = strings;
        this.cliStrings = strings.from("HumanReadable.CallLevel");
        this.messages = strings.from("HumanReadable.Messages");
        this.appInfo = strings.from("MachineReadable.App");

        // Get the localised options, with all required fields enabled as well.
        // Note that we are not yet applying the options to any command line,
        // because we still want to exit if only the help screen shall be
        // displayed first, but of course, we do need the localised options
        // here.
        this.options = getOptions(this.cliStrings);

        // If no arguments are supplied, or --help is used, we will exit
        // after printing the help screen
        if (cmd.getOptions().length == 0 || cmd.hasOption("help")) {
            printHelp();
        }

        try {
            cmd = parser.parse(this.options, args);

            username = cmd.getOptionValue("user");

            // URL-decode the password (STiNE doesn't actually allow special
            // chars in passwords, but meh...)
            password = URLDecoder.decode(cmd.getOptionValue("pass"), "UTF-8");
            // Double-dash signals that the password shall be read from stdin
            if (password.equals("--")) {
                password = readPassword(messages.get("PasswordQuery"), messages.get("PasswordFallbackMsg"));
            }

            echoPages = cmd.hasOption("echo");

            // the cache-dir argument is optional, so we read it with a default
            // value
            calendarCache = Paths
                    .get(cmd.getOptionValue("cache-dir", strings.get("MachineReadable.Paths.CalendarCache")))
                    .toAbsolutePath();

            // output-argument is optional as well, but this time we check if
            // double-dash is specified (for echo to stdout)
            String outputStr = cmd.getOptionValue("output", strings.get("MachineReadable.Paths.OutputFile"));
            if (outputStr.equals("--")) {
                echoCalendar = true;
                outputFile = null;
            } else {
                echoCalendar = false;
                outputFile = Paths.get(outputStr).toAbsolutePath();
            }

        } catch (UnrecognizedOptionException e) {
            System.err.println(messages.get("UnrecognisedOption", e.getOption().toString()));
            this.printHelp();
        } catch (MissingOptionException e) {
            // e.getMissingOptions() is just extremely horribly designed and
            // here is why:
            //
            // It returns an unparametrised list, to make your job especially
            // hard, whose elements may be:
            // - String-instances, if there are single independent options
            // missing (NOT the stupid Option itself, just why????)
            // - OptionGroup-instances, if there are whole OptionGroups missing
            // (This time the actual OptionGroup and not an unparametrised Set
            // that may or may not contain an Option, how inconsequential).
            // - Basically anything because the programmer who wrote that
            // function was clearly high and is most probably not to be trusted.
            //
            // This makes the job of actually displaying all the options as a
            // comma-separated list unnecessarily hard and hence leads to this
            // ugly contraption of Java-8-statements. But hey, at least it's not
            // as ugly as the Java 7 version (for me at least).
            // Sorry!
            // TODO: Write better code.
            // TODO: Write my own command line interpreter, with blackjack and
            // hookers.
            try {
                System.err.println(messages.get("MissingRequiredOption", ((List<?>) (e.getMissingOptions())).stream()
                        .filter(Object.class::isInstance).map(Object.class::cast).flatMap(o -> {
                            if (o instanceof String) {
                                return Collections.singletonList(options.getOption((String) o)).stream();
                            } else {
                                return ((OptionGroup) o).getOptions().stream();
                            }
                        }).filter(Option.class::isInstance).map(Option.class::cast).map(o -> o.getLongOpt())
                        .collect(Collectors.joining(", "))));
                this.printHelp();
            } catch (Exception totallyMoronicException) {
                throw new RuntimeException("I hate 3rd party libraries!", totallyMoronicException);
            }
        } catch (MissingArgumentException e) {
            System.err.println(messages.get("MissingRequiredArgument", e.getOption().getLongOpt()));
            this.printHelp();
        } catch (ParseException e) {
            System.err.println(messages.get("CallLevelParsingException", e.getMessage()));
        }

        this.username = username;
        this.password = password;
        this.echoPages = echoPages;
        this.calendarCache = calendarCache;
        this.outputFile = outputFile;
        this.echoCalendar = echoCalendar;
    }

    /**
     * Gets the password from stdin
     * 
     * @param query
     *            the query to the user
     * @param errorMsg
     *            the message to display when the input gets somehow redirected
     *            and System.console() becomes null.
     * @return the entered password
     */
    private static String readPassword(String query, String errorMsg) {
        final String result;
        if (System.console() != null) {
            System.err.print(query);
            result = new String(System.console().readPassword());
        } else {
            System.err.println(errorMsg);
            System.err.print(query);
            Scanner scanner = new Scanner(System.in);
            result = scanner.nextLine();
            scanner.close();
        }
        System.err.println();
        return result;
    }

    /**
     * Prints the help screen for the current Options instance and exits the
     * application
     */
    private void printHelp() {
        HelpFormatter formatter = new HelpFormatter();
        formatter.setOptionComparator(null);
        formatter.setDescPadding(4);
        formatter.setLeftPadding(2);
        formatter.setLongOptSeparator("=");
        formatter.setLongOptPrefix(" --");
        formatter.setSyntaxPrefix(cliStrings.get("Usage"));
        formatter.printHelp(cliStrings.get("UsageTemplate", appInfo.get("Name")),
                cliStrings.get("HelpHeader", appInfo.get("Name"), appInfo.get("Version")), this.options,
                cliStrings.get("HelpFooter", cliStrings.get("Author"), cliStrings.get("License"),
                        appInfo.get("ProjectPage")),
                true);
        System.exit(0);
    }

    /**
     * Acquires the Options with the given localisation from the StringProvider.
     * We already need a complete Options instance before we know which language
     * we are using, so passing null will simply not add any descriptions or
     * required options. These initial options should not be printed with a
     * HelpFormatter.
     */
    private Options getOptions(StringProvider strings) {
        final Options options = new Options();

        final UnaryOperator<String> getDescription = (key -> strings == null ? "" : strings.get(key));

        // @formatter:off
        options.addOption(Option.builder("h")
                                .longOpt("help")
                                .desc(getDescription.apply("HelpDescription"))
                                .build());

        options.addOption(Option.builder("l")
                                .longOpt("language")
                                .hasArg()
                                .argName("en|de")
                                .desc(getDescription.apply("LangDescription"))
                                .build());

        options.addOption(Option.builder("u")
                                .longOpt("user")
                                .required(strings != null)
                                .hasArg()
                                .argName("user")
                                .desc(getDescription.apply("UserDescription"))
                                .build());

        options.addOption(Option.builder("p")
                                .longOpt("pass")
                                .required(strings != null)
                                .hasArg()
                                .argName("pass")
                                .desc(getDescription.apply("PassDescription"))
                                .build());
        
        options.addOption(Option.builder("e")
                                .longOpt("echo")
                                .desc(getDescription.apply("EchoDescription"))
                                .build());

        options.addOption(Option.builder("c")
                                .longOpt("cache-dir")
                                .hasArg()
                                .argName("dir")
                                .desc(getDescription.apply("CacheDirDescription"))
                                .build());
        
        options.addOption(Option.builder("o")
                                .longOpt("output")
                                .hasArg()
                                .argName("file")
                                .desc(getDescription.apply("OutputDescription"))
                                .build());
        // @formatter:on

        return options;
    }

    /**
     * @return the username (STiNE)
     */
    public String getUsername() {
        return username;
    }

    /**
     * @return the password (STiNE)
     */
    public String getPassword() {
        return password;
    }

    /**
     * @return Determines whether the pages shall be echo'ed during browsing
     */
    public boolean isEchoPages() {
        return echoPages;
    }

    /**
     * @return the calendar cache directory
     */
    public Path getCalendarCache() {
        return calendarCache;
    }

    /**
     * @return the output filename
     */
    public Path getOutputFile() {
        return outputFile;
    }

    /**
     * @return Determines whether the calender shall be echo'ed to stdout at the
     *         end (true) or saved to file (false) according to
     *         getOutputFilename().
     */
    public boolean isEchoCalendar() {
        return echoCalendar;
    }

    /**
     * @return the {@link StringProvider} according to the specified --language
     *         argument
     */
    public StringProvider getStringProvider() {
        return strings;
    }
}
