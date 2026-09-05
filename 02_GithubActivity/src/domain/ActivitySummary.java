package domain;

import java.time.Instant;

/**
 * Every event of one kind against one repository, rolled up.
 * "Three pushes of one commit each" and "one push of three commits"
 * are the same story to a reader, and this is where that rule lives.
 */
public record ActivitySummary(ActivityType type, RepositoryName repository, int count, Instant lastOccurredAt) {

    public ActivitySummary {
        if (count < 1) {
            throw new IllegalArgumentException("A summary must cover at least one event, got: " + count);
        }
    }
}
