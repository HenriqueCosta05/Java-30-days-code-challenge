package domain;

import java.time.Instant;
import java.util.Objects;

/**
 * A single thing a user did in a repository at a point in time.
 */
public final class ActivityEvent {

    private final ActivityType type;
    private final RepositoryName repository;
    private final int count;
    private final Instant occurredAt;

    private ActivityEvent(ActivityType type, RepositoryName repository, int count, Instant occurredAt) {
        this.type = type;
        this.repository = repository;
        this.count = count;
        this.occurredAt = occurredAt;
    }

    public static ActivityEvent of(ActivityType type, RepositoryName repository, int count, Instant occurredAt) {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(repository, "repository");
        Objects.requireNonNull(occurredAt, "occurredAt");
        if (count < 1) {
            throw new IllegalArgumentException("An activity event must count at least once, got: " + count);
        }
        if (!type.isCountable() && count != 1) {
            throw new IllegalArgumentException(type + " happens exactly once per event, got count: " + count);
        }
        return new ActivityEvent(type, repository, count, occurredAt);
    }

    public static ActivityEvent single(ActivityType type, RepositoryName repository, Instant occurredAt) {
        return of(type, repository, 1, occurredAt);
    }

    public ActivityType type() {
        return type;
    }

    public RepositoryName repository() {
        return repository;
    }

    public int count() {
        return count;
    }

    public Instant occurredAt() {
        return occurredAt;
    }
}
