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

public class CallLevelWrapper {

    private final StringProvider strings;
    private final StringProvider cliStrings;
    private final StringProvider messages;

    final private Options options;

    private String user;
    private String pass;
    private boolean echoPages;
    private Path cache_dir;
    private Path output;
    private boolean echoCalendar;

    public CallLevelWrapper(String[] args) throws IOException {

        strings = getLanguage(args, getOptions(null));
        cliStrings = strings.from("HumanReadable.CallLevel");
        messages = strings.from("HumanReadable.Messages");

        options = getOptions(cliStrings);

        CommandLineParser parser = new DefaultParser();
        CommandLine cmd;
        try {
            cmd = parser.parse(options, args);

            if (cmd.getOptions().length == 0 || cmd.hasOption("help")) {
                this.printHelp();
            }

            this.user = cmd.getOptionValue("user");

            this.pass = URLDecoder.decode(cmd.getOptionValue("pass"), "UTF-8");
            if (this.pass.equals("--")) {
                pass = readPassword(messages.get("PasswordQuery"), messages.get("PasswordFallbackMsg"));
            }

            this.echoPages = cmd.hasOption("echo");

            this.cache_dir = Paths
                    .get(cmd.getOptionValue("cache-dir", strings.get("MachineReadable.Paths.CalendarCache")))
                    .toAbsolutePath();

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

    private void printHelp() {
        HelpFormatter formatter = new HelpFormatter();
        formatter.setOptionComparator(null);
        formatter.setDescPadding(4);
        formatter.setLeftPadding(2);
        formatter.setLongOptSeparator("=");
        formatter.setLongOptPrefix(" --");
        formatter.setSyntaxPrefix(cliStrings.get("Usage"));
        formatter
                .printHelp(
                        cliStrings.get("AppName"), cliStrings.get("HelpHeader"), options, cliStrings.get("HelpFooter",
                                cliStrings.get("Author"), cliStrings.get("License"), cliStrings.get("ProjectPage")),
                        true);
        System.exit(0);
    }

    /**
     * Acquires the StringProvider instance for the specified language.
     * 
     * @param args
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
     * @return the user
     */
    public String getUser() {
        return user;
    }

    /**
     * @return the pass
     */
    public String getPass() {
        return pass;
    }

    /**
     * @return the echoPages
     */
    public boolean isEchoPages() {
        return echoPages;
    }

    /**
     * @return the cache_dir
     */
    public Path getCache_dir() {
        return cache_dir;
    }

    /**
     * @return the output
     */
    public Path getOutput() {
        return output;
    }

    /**
     * @return the echoCalendar
     */
    public boolean isEchoCalendar() {
        return echoCalendar;
    }

}
