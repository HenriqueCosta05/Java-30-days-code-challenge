import static testing.Assertions.assertEquals;
import static testing.Assertions.assertThrows;
import static testing.Assertions.assertTrue;
import static testing.Assertions.check;
import static testing.Assertions.section;

import application.showuseractivity.ActivityCache;
import application.showuseractivity.CachedActivity;
import application.showuseractivity.GithubActivityGateway;
import application.showuseractivity.ShowUserActivityRequest;
import application.showuseractivity.ShowUserActivityResponse;
import application.showuseractivity.ShowUserActivityUseCase;
import application.showuseractivity.UserNotFoundException;
import domain.ActivityEvent;
import domain.ActivityFeed;
import domain.ActivitySummary;
import domain.ActivityType;
import domain.GithubUsername;
import domain.InvalidUsernameException;
import domain.RepositoryName;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Entities and use case, exercised without network, files or a framework.
 */
final class CoreTests {

    private static final Instant NOW = Instant.parse("2026-09-05T12:00:00Z");
    private static final RepositoryName ROADMAP = RepositoryName.of("kamranahmedse/developer-roadmap");

    private CoreTests() {
    }

    static void run() {
        section("Domain: GithubUsername");
        check("accepts a normal login", () -> assertEquals("kamranahmedse", GithubUsername.of("kamranahmedse").value()));
        check("accepts hyphens inside the login", () -> assertEquals("a-b", GithubUsername.of("a-b").value()));
        check("rejects an empty login", () -> assertThrows(InvalidUsernameException.class, () -> GithubUsername.of(" ")));
        check("rejects a leading hyphen", () -> assertThrows(InvalidUsernameException.class, () -> GithubUsername.of("-nope")));
        check("rejects consecutive hyphens", () -> assertThrows(InvalidUsernameException.class, () -> GithubUsername.of("a--b")));
        check("rejects illegal characters", () -> assertThrows(InvalidUsernameException.class, () -> GithubUsername.of("hen ri")));
        check("rejects logins longer than 39 characters", () -> assertThrows(InvalidUsernameException.class, () -> GithubUsername.of("a".repeat(40))));

        section("Domain: ActivityEvent");
        check("counts at least one occurrence", () -> assertThrows(IllegalArgumentException.class,
                () -> ActivityEvent.of(ActivityType.PUSHED_COMMITS, ROADMAP, 0, NOW)));
        check("refuses a count on a one-off activity", () -> assertThrows(IllegalArgumentException.class,
                () -> ActivityEvent.of(ActivityType.STARRED, ROADMAP, 2, NOW)));

        section("Domain: ActivityFeed");
        check("orders events newest first", () -> {
            ActivityFeed feed = ActivityFeed.of(List.of(
                    ActivityEvent.single(ActivityType.STARRED, ROADMAP, NOW.minusSeconds(600)),
                    ActivityEvent.single(ActivityType.FORKED, ROADMAP, NOW)));
            assertEquals(ActivityType.FORKED, feed.events().get(0).type());
        });
        check("rolls repeated pushes into one summary", () -> {
            ActivityFeed feed = ActivityFeed.of(List.of(
                    ActivityEvent.of(ActivityType.PUSHED_COMMITS, ROADMAP, 2, NOW),
                    ActivityEvent.of(ActivityType.PUSHED_COMMITS, ROADMAP, 1, NOW.minusSeconds(60))));
            List<ActivitySummary> summaries = feed.summarize();
            assertEquals(1, summaries.size());
            assertEquals(3, summaries.get(0).count());
        });
        check("keeps different repositories apart", () -> {
            ActivityFeed feed = ActivityFeed.of(List.of(
                    ActivityEvent.of(ActivityType.PUSHED_COMMITS, ROADMAP, 1, NOW),
                    ActivityEvent.of(ActivityType.PUSHED_COMMITS, RepositoryName.of("other/repo"), 1, NOW.minusSeconds(1))));
            assertEquals(2, feed.summarize().size());
        });
        check("filters by activity type", () -> {
            ActivityFeed feed = ActivityFeed.of(List.of(
                    ActivityEvent.single(ActivityType.STARRED, ROADMAP, NOW),
                    ActivityEvent.of(ActivityType.PUSHED_COMMITS, ROADMAP, 1, NOW.minusSeconds(60))));
            assertEquals(1, feed.filterBy(ActivityType.STARRED).events().size());
        });

        section("Use case: show user activity");
        check("fetches from GitHub and remembers the result", () -> {
            RecordingGateway gateway = new RecordingGateway(List.of(
                    ActivityEvent.of(ActivityType.PUSHED_COMMITS, ROADMAP, 3, NOW)));
            InMemoryCache cache = new InMemoryCache();
            ShowUserActivityResponse response = useCase(gateway, cache, NOW)
                    .execute(ShowUserActivityRequest.forUser("kamranahmedse"));
            assertEquals(1, gateway.calls);
            assertEquals(false, response.servedFromCache());
            assertEquals(3, response.summaries().get(0).count());
            assertTrue("the feed was cached", cache.find(GithubUsername.of("kamranahmedse")).isPresent());
        });
        check("serves a fresh cache without calling GitHub", () -> {
            RecordingGateway gateway = new RecordingGateway(List.of());
            InMemoryCache cache = new InMemoryCache();
            cache.store(GithubUsername.of("kamranahmedse"),
                    List.of(ActivityEvent.single(ActivityType.STARRED, ROADMAP, NOW)), NOW.minusSeconds(60));
            ShowUserActivityResponse response = useCase(gateway, cache, NOW)
                    .execute(ShowUserActivityRequest.forUser("kamranahmedse"));
            assertEquals(0, gateway.calls);
            assertEquals(true, response.servedFromCache());
        });
        check("goes back to GitHub once the cache is stale", () -> {
            RecordingGateway gateway = new RecordingGateway(List.of());
            InMemoryCache cache = new InMemoryCache();
            cache.store(GithubUsername.of("kamranahmedse"),
                    List.of(ActivityEvent.single(ActivityType.STARRED, ROADMAP, NOW)), NOW.minusSeconds(3600));
            useCase(gateway, cache, NOW).execute(ShowUserActivityRequest.forUser("kamranahmedse"));
            assertEquals(1, gateway.calls);
        });
        check("refresh ignores a fresh cache", () -> {
            RecordingGateway gateway = new RecordingGateway(List.of());
            InMemoryCache cache = new InMemoryCache();
            cache.store(GithubUsername.of("kamranahmedse"),
                    List.of(ActivityEvent.single(ActivityType.STARRED, ROADMAP, NOW)), NOW);
            useCase(gateway, cache, NOW).execute(
                    new ShowUserActivityRequest("kamranahmedse", null, null, true));
            assertEquals(1, gateway.calls);
        });
        check("applies the activity type filter", () -> {
            RecordingGateway gateway = new RecordingGateway(List.of(
                    ActivityEvent.single(ActivityType.STARRED, ROADMAP, NOW),
                    ActivityEvent.of(ActivityType.PUSHED_COMMITS, ROADMAP, 1, NOW.minusSeconds(60))));
            ShowUserActivityResponse response = useCase(gateway, new InMemoryCache(), NOW).execute(
                    new ShowUserActivityRequest("kamranahmedse", ActivityType.STARRED, null, false));
            assertEquals(1, response.summaries().size());
            assertEquals(ActivityType.STARRED, response.summaries().get(0).type());
        });
        check("applies the limit", () -> {
            RecordingGateway gateway = new RecordingGateway(List.of(
                    ActivityEvent.single(ActivityType.STARRED, ROADMAP, NOW),
                    ActivityEvent.single(ActivityType.FORKED, ROADMAP, NOW.minusSeconds(60)),
                    ActivityEvent.single(ActivityType.MADE_PUBLIC, ROADMAP, NOW.minusSeconds(120))));
            ShowUserActivityResponse response = useCase(gateway, new InMemoryCache(), NOW).execute(
                    new ShowUserActivityRequest("kamranahmedse", null, 2, false));
            assertEquals(2, response.summaries().size());
        });
        check("rejects an invalid username before calling GitHub", () -> {
            RecordingGateway gateway = new RecordingGateway(List.of());
            assertThrows(InvalidUsernameException.class, () -> useCase(gateway, new InMemoryCache(), NOW)
                    .execute(ShowUserActivityRequest.forUser("not a login")));
            assertEquals(0, gateway.calls);
        });
        check("lets a gateway failure through untouched", () -> {
            GithubActivityGateway failing = username -> {
                throw new UserNotFoundException(username);
            };
            assertThrows(UserNotFoundException.class, () -> useCase(failing, new InMemoryCache(), NOW)
                    .execute(ShowUserActivityRequest.forUser("ghost")));
        });
        check("reports an empty feed rather than failing", () -> {
            ShowUserActivityResponse response = useCase(new RecordingGateway(List.of()), new InMemoryCache(), NOW)
                    .execute(ShowUserActivityRequest.forUser("quiet"));
            assertTrue("no summaries", response.summaries().isEmpty());
        });
    }

    private static ShowUserActivityUseCase useCase(GithubActivityGateway gateway, ActivityCache cache, Instant now) {
        return new ShowUserActivityUseCase(gateway, cache, Clock.fixed(now, ZoneOffset.UTC), Duration.ofMinutes(10));
    }

    private static final class RecordingGateway implements GithubActivityGateway {

        private final List<ActivityEvent> events;
        private int calls;

        private RecordingGateway(List<ActivityEvent> events) {
            this.events = events;
        }

        @Override
        public List<ActivityEvent> fetchRecentActivity(GithubUsername username) {
            calls++;
            return new ArrayList<>(events);
        }
    }

    private static final class InMemoryCache implements ActivityCache {

        private final Map<String, CachedActivity> entries = new HashMap<>();

        @Override
        public Optional<CachedActivity> find(GithubUsername username) {
            return Optional.ofNullable(entries.get(username.value().toLowerCase()));
        }

        @Override
        public void store(GithubUsername username, List<ActivityEvent> events, Instant fetchedAt) {
            entries.put(username.value().toLowerCase(), new CachedActivity(events, fetchedAt));
        }
    }
}
