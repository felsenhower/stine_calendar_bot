package felsenhower.stine_calendar_bot.main;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
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
 * Acts as a wrapper for console interaction
 *
 */
public class CallLevelWrapper {

    private final StringProvider strings;
    private final StringProvider cliStrings;
    private final StringProvider messages;
    private final StringProvider appInfo;

    final private Options options;

    private String user;
    private String pass;
    private boolean echoPages;
    private Path calendarCache;
    private Path output;
    private boolean echoCalendar;

    /**
     * Creates a new instance
     * 
     * @param args
     *            the submitted args from the main method.
     */
    @SuppressWarnings("unchecked")
    public CallLevelWrapper(String[] args) throws IOException {

        // Get the StringProvider
        strings = getLanguage(args, getOptions(null));
        cliStrings = strings.from("HumanReadable.CallLevel");
        messages = strings.from("HumanReadable.Messages");
        appInfo = strings.from("MachineReadable.App");

        // Get the localised option
        options = getOptions(cliStrings);

        CommandLineParser parser = new DefaultParser();
        CommandLine cmd;
        try {
            cmd = parser.parse(options, args);

            // If no arguments are supplied, or --help is used, we will exit
            // after printing the help screen
            if (cmd.getOptions().length == 0 || cmd.hasOption("help")) {
                this.printHelp();
            }

            this.user = cmd.getOptionValue("user");

            // URL-decode the password (STiNE doesn't actually allow special
            // chars in passwords, but meh...)
            this.pass = URLDecoder.decode(cmd.getOptionValue("pass"), "UTF-8");
            // Double-dash signals that the password shall be read from stdin
            if (this.pass.equals("--")) {
                pass = readPassword(messages.get("PasswordQuery"), messages.get("PasswordFallbackMsg"));
            }

            this.echoPages = cmd.hasOption("echo");

            // the cache-dir argument is optional, so we read it with a default
            // value
            this.calendarCache = Paths
                    .get(cmd.getOptionValue("cache-dir", strings.get("MachineReadable.Paths.CalendarCache")))
                    .toAbsolutePath();

            // output-argument is optional as well, but this time we check if
            // double-dash is specified (for echo to stdout)
            String outputStr = cmd.getOptionValue("output", strings.get("MachineReadable.Paths.OutputFile"));
            if (outputStr.equals("--")) {
                this.echoCalendar = true;
                this.output = null;
            } else {
                this.echoCalendar = false;
                this.output = Paths.get(outputStr).toAbsolutePath();
            }

        } catch (UnrecognizedOptionException e) {
            System.err.println(messages.get("UnrecognisedOption", e.getOption().toString()));
            this.printHelp();
        } catch (MissingOptionException e) {
            // e.getMissingOptions() is just extremely horribly designed and
            // here is why:
            //
            // It returns an unparametrised list, to make your job especially
            // hard, whose element may be:
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
            // ugly contraption of Java-8-statements which btw. is the one and
            // only cause for the @SuppressWarnings("unchecked") over the method
            // header. I just couldn't stand this 5 lines long mess of yellow
            // underlined code in Eclipse because it's clearly (?) not
            // completely obvious that all the resulting streams are of the type
            // <Option>.
            // Sorry!
            // TODO: Write better code.
            // TODO: Write my own command line interpreter, with blackjack and
            // hookers.
            System.err.println(messages.get("MissingRequiredOption",
                    e.getMissingOptions().stream()
                            .flatMap(o -> (o instanceof String
                                    ? Collections.singletonList(options.getOption((String) o)).stream()
                                    : ((OptionGroup) o).getOptions().stream()))
                            .map(o -> ((Option) o).getLongOpt()).collect(Collectors.joining(", "))));
            this.printHelp();
        } catch (MissingArgumentException e) {
            System.err.println(messages.get("MissingRequiredArgument", e.getOption().getLongOpt()));
            this.printHelp();
        } catch (ParseException e) {
            System.err.println(messages.get("CallLevelParsingException", e.getMessage()));
        }
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
                cliStrings.get("HelpHeader", appInfo.get("Name"), appInfo.get("Version")), options,
                cliStrings.get("HelpFooter", cliStrings.get("Author"), cliStrings.get("License"),
                        appInfo.get("ProjectPage")),
                true);
        System.exit(0);
    }

    /**
     * Acquires the StringProvider instance for the specified language.
     * 
     * @param args
     *            the arguments to extract the language from
     * @return a {@link StringProvider}
     */
    private static StringProvider getLanguage(String[] args, Options options) {

        try {
            CommandLineParser parser = new DefaultParser();
            CommandLine cmd = parser.parse(options, args);
            if (cmd.hasOption("language")) {
                String lang = cmd.getOptionValue("language").toLowerCase();
                if (lang.equals("de")) {
                    return new StringProvider(Locale.GERMAN);
                } else {
                    StringProvider strings = new StringProvider(Locale.ENGLISH);
                    if (!lang.equals("en")) {
                        System.err.println(strings.get("HumanReadable.CallLevel.LangNotRecognised", lang));
                    }
                    return strings;
                }
            } else {
                return new StringProvider(new Locale(Locale.getDefault().getLanguage()));
            }
        } catch (ParseException e) {
            return new StringProvider(Locale.ENGLISH);
        }
    }

    /**
     * @return the {@link StringProvider} according to the specified --language
     *         argument
     */
    public StringProvider getStringProvider() {
        return strings;
    }

    /**
     * @return the username (STiNE)
     */
    public String getUser() {
        return user;
    }

    /**
     * @return the password (STiNE)
     */
    public String getPass() {
        return pass;
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
    public Path getOutputFilename() {
        return output;
    }

    /**
     * @return Determines whether the calender shall be echo'ed to stdout at the
     *         end (true) or saved to file (false) according to
     *         getOutputFilename().
     */
    public boolean isEchoCalendar() {
        return echoCalendar;
    }

}
