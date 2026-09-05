package adapters.cli;

import application.showuseractivity.ActivityFetchFailedException;
import application.showuseractivity.ShowUserActivityRequest;
import application.showuseractivity.ShowUserActivityResponse;
import application.showuseractivity.ShowUserActivityUseCase;
import application.showuseractivity.UserNotFoundException;
import domain.ActivityType;
import domain.InvalidUsernameException;
import java.io.PrintStream;
import java.time.Clock;
import java.util.List;
import java.util.Optional;

/**
 * Translates command line input into a use case call and its result into
 * terminal output and an exit code. Holds no business rule of its own.
 */
public final class GithubActivityCliController {

    public static final int EXIT_SUCCESS = 0;
    public static final int EXIT_USAGE_ERROR = 1;
    public static final int EXIT_USER_NOT_FOUND = 2;
    public static final int EXIT_FETCH_FAILED = 3;

    private static final String USAGE = """
            Usage: github-activity <username> [options]

            Options:
              --type=<activity>  show only one kind of activity
              --limit=<number>   show at most this many lines
              --refresh          ignore the cache and ask GitHub again
              --help             show this message""";

    private final ShowUserActivityUseCase showUserActivity;
    private final ActivityPresenter presenter;
    private final Clock clock;
    private final PrintStream out;
    private final PrintStream err;

    public GithubActivityCliController(ShowUserActivityUseCase showUserActivity, ActivityPresenter presenter,
                                       Clock clock, PrintStream out, PrintStream err) {
        this.showUserActivity = showUserActivity;
        this.presenter = presenter;
        this.clock = clock;
        this.out = out;
        this.err = err;
    }

    public int run(String[] arguments) {
        if (arguments.length == 0 || isHelpRequest(arguments)) {
            out.println(USAGE);
            return arguments.length == 0 ? EXIT_USAGE_ERROR : EXIT_SUCCESS;
        }

        String username = null;
        ActivityType activityType = null;
        Integer limit = null;
        boolean refresh = false;

        for (String argument : arguments) {
            if (!argument.startsWith("--")) {
                if (username != null) {
                    return usageError("Only one username may be given, got also: " + argument);
                }
                username = argument;
                continue;
            }
            if (argument.equals("--refresh")) {
                refresh = true;
            } else if (argument.startsWith("--type=")) {
                Optional<ActivityType> parsed = ActivityTypeArgument.parse(value(argument));
                if (parsed.isEmpty()) {
                    return usageError("Unknown activity type: " + value(argument)
                            + System.lineSeparator() + "Supported types: " + ActivityTypeArgument.supportedValues());
                }
                activityType = parsed.get();
            } else if (argument.startsWith("--limit=")) {
                try {
                    limit = Integer.parseInt(value(argument));
                } catch (NumberFormatException notANumber) {
                    return usageError("--limit expects a whole number, got: " + value(argument));
                }
                if (limit < 1) {
                    return usageError("--limit must be at least 1, got: " + limit);
                }
            } else {
                return usageError("Unknown option: " + argument);
            }
        }

        if (username == null) {
            return usageError("A GitHub username is required.");
        }

        try {
            ShowUserActivityResponse response =
                    showUserActivity.execute(new ShowUserActivityRequest(username, activityType, limit, refresh));
            print(presenter.present(response, clock.instant()));
            return EXIT_SUCCESS;
        } catch (InvalidUsernameException invalid) {
            return usageError(invalid.getMessage());
        } catch (UserNotFoundException notFound) {
            err.println("Error: " + notFound.getMessage());
            return EXIT_USER_NOT_FOUND;
        } catch (ActivityFetchFailedException failed) {
            err.println("Error: " + failed.getMessage());
            return EXIT_FETCH_FAILED;
        }
    }

    private void print(List<String> lines) {
        lines.forEach(out::println);
    }

    private static boolean isHelpRequest(String[] arguments) {
        for (String argument : arguments) {
            if (argument.equals("--help") || argument.equals("-h")) {
                return true;
            }
        }
        return false;
    }

    private static String value(String argument) {
        return argument.substring(argument.indexOf('=') + 1);
    }

    private int usageError(String message) {
        err.println("Error: " + message);
        err.println(USAGE);
        return EXIT_USAGE_ERROR;
    }
}
