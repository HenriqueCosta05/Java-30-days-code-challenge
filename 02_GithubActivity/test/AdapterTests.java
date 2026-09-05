import static testing.Assertions.assertEquals;
import static testing.Assertions.assertTrue;
import static testing.Assertions.check;
import static testing.Assertions.section;

import adapters.cli.ActivityTextPresenter;
import adapters.cli.GithubActivityCliController;
import application.showuseractivity.ActivityCache;
import application.showuseractivity.CachedActivity;
import application.showuseractivity.GithubActivityGateway;
import application.showuseractivity.ShowUserActivityResponse;
import application.showuseractivity.ShowUserActivityUseCase;
import application.showuseractivity.UserNotFoundException;
import domain.ActivityEvent;
import domain.ActivitySummary;
import domain.ActivityType;
import domain.GithubUsername;
import domain.RepositoryName;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

/**
 * Presenter and controller: translation only, no business rules.
 */
final class AdapterTests {

    private static final Instant NOW = Instant.parse("2026-09-05T12:00:00Z");
    private static final RepositoryName ROADMAP = RepositoryName.of("kamranahmedse/developer-roadmap");

    private AdapterTests() {
    }

    static void run() {
        section("Presenter: activity as text");
        check("describes a push with its commit count", () ->
                assertEquals("- Pushed 3 commits to kamranahmedse/developer-roadmap",
                        present(new ActivitySummary(ActivityType.PUSHED_COMMITS, ROADMAP, 3, NOW))));
        check("uses the singular for a single commit", () ->
                assertEquals("- Pushed 1 commit to kamranahmedse/developer-roadmap",
                        present(new ActivitySummary(ActivityType.PUSHED_COMMITS, ROADMAP, 1, NOW))));
        check("describes a new issue", () ->
                assertEquals("- Opened a new issue in kamranahmedse/developer-roadmap",
                        present(new ActivitySummary(ActivityType.OPENED_ISSUE, ROADMAP, 1, NOW))));
        check("describes several new issues", () ->
                assertEquals("- Opened 2 new issues in kamranahmedse/developer-roadmap",
                        present(new ActivitySummary(ActivityType.OPENED_ISSUE, ROADMAP, 2, NOW))));
        check("describes a star", () ->
                assertEquals("- Starred kamranahmedse/developer-roadmap",
                        present(new ActivitySummary(ActivityType.STARRED, ROADMAP, 1, NOW))));
        check("says so when there is nothing to show", () -> {
            List<String> lines = new ActivityTextPresenter().present(
                    new ShowUserActivityResponse("quiet", List.of(), false, NOW), NOW);
            assertEquals("- No recent public activity.", lines.get(1));
        });
        check("says so when a filter matched nothing", () -> {
            List<String> lines = new ActivityTextPresenter().present(
                    new ShowUserActivityResponse("quiet", List.of(), false, NOW, ActivityType.STARRED), NOW);
            assertEquals("- No recent public activity of that kind.", lines.get(1));
        });
        check("marks a cached feed with its age", () -> {
            List<String> lines = new ActivityTextPresenter().present(
                    new ShowUserActivityResponse("kamranahmedse", List.of(), true, NOW.minusSeconds(180)), NOW);
            assertEquals("Recent activity for kamranahmedse (cached 3 minutes ago):", lines.get(0));
        });

        section("Controller: command line");
        check("prints the activity and succeeds", () -> {
            Output output = new Output();
            int exitCode = controller(gatewayReturning(
                    ActivityEvent.of(ActivityType.PUSHED_COMMITS, ROADMAP, 3, NOW)), output)
                    .run(new String[]{"kamranahmedse"});
            assertEquals(GithubActivityCliController.EXIT_SUCCESS, exitCode);
            assertTrue("the push is listed", output.out().contains("Pushed 3 commits"));
        });
        check("without arguments it explains the usage", () -> {
            Output output = new Output();
            int exitCode = controller(gatewayReturning(), output).run(new String[]{});
            assertEquals(GithubActivityCliController.EXIT_USAGE_ERROR, exitCode);
            assertTrue("usage is shown", output.out().contains("Usage: github-activity"));
        });
        check("--help succeeds", () -> {
            Output output = new Output();
            assertEquals(GithubActivityCliController.EXIT_SUCCESS,
                    controller(gatewayReturning(), output).run(new String[]{"--help"}));
        });
        check("rejects an unknown option", () -> {
            Output output = new Output();
            assertEquals(GithubActivityCliController.EXIT_USAGE_ERROR,
                    controller(gatewayReturning(), output).run(new String[]{"someone", "--verbose"}));
        });
        check("rejects an unknown activity type", () -> {
            Output output = new Output();
            assertEquals(GithubActivityCliController.EXIT_USAGE_ERROR,
                    controller(gatewayReturning(), output).run(new String[]{"someone", "--type=dancing"}));
            assertTrue("the supported types are listed", output.err().contains("Supported types"));
        });
        check("rejects a limit that is not a number", () -> {
            Output output = new Output();
            assertEquals(GithubActivityCliController.EXIT_USAGE_ERROR,
                    controller(gatewayReturning(), output).run(new String[]{"someone", "--limit=many"}));
        });
        check("rejects an invalid username as a usage error", () -> {
            Output output = new Output();
            assertEquals(GithubActivityCliController.EXIT_USAGE_ERROR,
                    controller(gatewayReturning(), output).run(new String[]{"not a login"}));
        });
        check("reports an unknown user with its own exit code", () -> {
            Output output = new Output();
            GithubActivityGateway missing = username -> {
                throw new UserNotFoundException(username);
            };
            assertEquals(GithubActivityCliController.EXIT_USER_NOT_FOUND,
                    controller(missing, output).run(new String[]{"ghost"}));
            assertTrue("the error is explained", output.err().contains("No GitHub user named"));
        });
        check("filters by activity type", () -> {
            Output output = new Output();
            controller(gatewayReturning(
                    ActivityEvent.single(ActivityType.STARRED, ROADMAP, NOW),
                    ActivityEvent.of(ActivityType.PUSHED_COMMITS, ROADMAP, 1, NOW.minusSeconds(60))), output)
                    .run(new String[]{"kamranahmedse", "--type=star"});
            assertTrue("only the star is listed",
                    output.out().contains("Starred") && !output.out().contains("Pushed"));
        });
    }

    private static String present(ActivitySummary summary) {
        return new ActivityTextPresenter()
                .present(new ShowUserActivityResponse("kamranahmedse", List.of(summary), false, NOW), NOW)
                .get(1);
    }

    private static GithubActivityGateway gatewayReturning(ActivityEvent... events) {
        return username -> List.of(events);
    }

    private static GithubActivityCliController controller(GithubActivityGateway gateway, Output output) {
        Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
        return new GithubActivityCliController(
                new ShowUserActivityUseCase(gateway, new NoCache(), clock, Duration.ofMinutes(10)),
                new ActivityTextPresenter(),
                clock,
                output.outStream,
                output.errStream);
    }

    /**
     * Captures what the controller writes, so the test never touches the console.
     */
    private static final class Output {

        private final ByteArrayOutputStream outBuffer = new ByteArrayOutputStream();
        private final ByteArrayOutputStream errBuffer = new ByteArrayOutputStream();
        private final PrintStream outStream = new PrintStream(outBuffer, true, StandardCharsets.UTF_8);
        private final PrintStream errStream = new PrintStream(errBuffer, true, StandardCharsets.UTF_8);

        private String out() {
            outStream.flush();
            return outBuffer.toString(StandardCharsets.UTF_8);
        }

        private String err() {
            errStream.flush();
            return errBuffer.toString(StandardCharsets.UTF_8);
        }
    }

    private static final class NoCache implements ActivityCache {

        @Override
        public Optional<CachedActivity> find(GithubUsername username) {
            return Optional.empty();
        }

        @Override
        public void store(GithubUsername username, List<ActivityEvent> events, Instant fetchedAt) {
            // The controller tests are not about caching.
        }
    }
}
