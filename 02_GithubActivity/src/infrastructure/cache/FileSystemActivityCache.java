package infrastructure.cache;

import application.showuseractivity.ActivityCache;
import application.showuseractivity.CachedActivity;
import domain.ActivityEvent;
import domain.ActivityType;
import domain.GithubUsername;
import domain.RepositoryName;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Keeps one tab separated file per user under a cache directory. The first
 * line holds the moment the feed was fetched, every other line one event.
 * Caching is best effort: a broken or unreadable file is simply a miss.
 */
public final class FileSystemActivityCache implements ActivityCache {

    private static final String SEPARATOR = "\t";

    private final Path directory;

    public FileSystemActivityCache(Path directory) {
        this.directory = directory;
    }

    @Override
    public Optional<CachedActivity> find(GithubUsername username) {
        Path file = fileFor(username);
        if (!Files.isReadable(file)) {
            return Optional.empty();
        }
        try {
            List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
            if (lines.isEmpty()) {
                return Optional.empty();
            }
            Instant fetchedAt = Instant.parse(lines.get(0).trim());
            List<ActivityEvent> events = new ArrayList<>();
            for (String line : lines.subList(1, lines.size())) {
                if (!line.isBlank()) {
                    events.add(readEvent(line));
                }
            }
            return Optional.of(new CachedActivity(events, fetchedAt));
        } catch (IOException | RuntimeException unusable) {
            return Optional.empty();
        }
    }

    @Override
    public void store(GithubUsername username, List<ActivityEvent> events, Instant fetchedAt) {
        StringBuilder content = new StringBuilder(fetchedAt.toString()).append(System.lineSeparator());
        for (ActivityEvent event : events) {
            content.append(event.type().name()).append(SEPARATOR)
                    .append(event.repository().value()).append(SEPARATOR)
                    .append(event.count()).append(SEPARATOR)
                    .append(event.occurredAt()).append(System.lineSeparator());
        }
        try {
            Files.createDirectories(directory);
            Files.writeString(fileFor(username), content.toString(), StandardCharsets.UTF_8);
        } catch (IOException notWritable) {
            // A cache that cannot be written must not stop the user from
            // seeing the activity that was just fetched.
        }
    }

    private static ActivityEvent readEvent(String line) {
        String[] fields = line.split(SEPARATOR, -1);
        if (fields.length != 4) {
            throw new IllegalStateException("Malformed cache entry: " + line);
        }
        return ActivityEvent.of(
                ActivityType.valueOf(fields[0]),
                RepositoryName.of(fields[1]),
                Integer.parseInt(fields[2]),
                Instant.parse(fields[3]));
    }

    private Path fileFor(GithubUsername username) {
        return directory.resolve(username.value().toLowerCase() + ".tsv");
    }
}
