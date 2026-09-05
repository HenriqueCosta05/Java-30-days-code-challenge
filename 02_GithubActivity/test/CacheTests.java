import static testing.Assertions.assertEquals;
import static testing.Assertions.assertTrue;
import static testing.Assertions.check;
import static testing.Assertions.section;

import application.showuseractivity.CachedActivity;
import domain.ActivityEvent;
import domain.ActivityType;
import domain.GithubUsername;
import domain.RepositoryName;
import infrastructure.cache.FileSystemActivityCache;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * The cache adapter against a real, throwaway directory.
 */
final class CacheTests {

    private static final Instant NOW = Instant.parse("2026-09-05T12:00:00Z");
    private static final GithubUsername USER = GithubUsername.of("kamranahmedse");
    private static final RepositoryName ROADMAP = RepositoryName.of("kamranahmedse/developer-roadmap");

    private CacheTests() {
    }

    static void run() {
        section("Infrastructure: file system cache");
        check("a missing entry is a miss", () -> inTemporaryDirectory(directory ->
                assertTrue("nothing cached yet", new FileSystemActivityCache(directory).find(USER).isEmpty())));
        check("stores and reads a feed back", () -> inTemporaryDirectory(directory -> {
            FileSystemActivityCache cache = new FileSystemActivityCache(directory);
            cache.store(USER, List.of(
                    ActivityEvent.of(ActivityType.PUSHED_COMMITS, ROADMAP, 3, NOW),
                    ActivityEvent.single(ActivityType.STARRED, ROADMAP, NOW.minusSeconds(60))), NOW);
            Optional<CachedActivity> cached = cache.find(USER);
            assertTrue("the feed came back", cached.isPresent());
            assertEquals(NOW, cached.get().fetchedAt());
            assertEquals(2, cached.get().events().size());
            assertEquals(3, cached.get().events().get(0).count());
        }));
        check("an empty feed round trips", () -> inTemporaryDirectory(directory -> {
            FileSystemActivityCache cache = new FileSystemActivityCache(directory);
            cache.store(USER, List.of(), NOW);
            assertTrue("an empty feed is still a hit", cache.find(USER).isPresent());
        }));
        check("a corrupted file is a miss, not a crash", () -> inTemporaryDirectory(directory -> {
            FileSystemActivityCache cache = new FileSystemActivityCache(directory);
            cache.store(USER, List.of(ActivityEvent.single(ActivityType.STARRED, ROADMAP, NOW)), NOW);
            write(directory.resolve("kamranahmedse.tsv"), "this is not a cache file");
            assertTrue("the broken file is ignored", cache.find(USER).isEmpty());
        }));
        check("the login is matched regardless of case", () -> inTemporaryDirectory(directory -> {
            FileSystemActivityCache cache = new FileSystemActivityCache(directory);
            cache.store(USER, List.of(ActivityEvent.single(ActivityType.STARRED, ROADMAP, NOW)), NOW);
            assertTrue("found under another spelling",
                    cache.find(GithubUsername.of("KamranAhmedse")).isPresent());
        }));
    }

    private interface DirectoryTest {

        void run(Path directory) throws IOException;
    }

    private static void inTemporaryDirectory(DirectoryTest test) {
        Path directory = null;
        try {
            directory = Files.createTempDirectory("github-activity-cache-test");
            test.run(directory);
        } catch (IOException failure) {
            throw new UncheckedIOException(failure);
        } finally {
            deleteRecursively(directory);
        }
    }

    private static void write(Path file, String content) throws IOException {
        Files.writeString(file, content, StandardCharsets.UTF_8);
    }

    private static void deleteRecursively(Path directory) {
        if (directory == null || !Files.exists(directory)) {
            return;
        }
        try (var paths = Files.walk(directory)) {
            paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException ignored) {
                    // A leftover temporary file must not fail the suite.
                }
            });
        } catch (IOException ignored) {
            // Same here.
        }
    }
}
