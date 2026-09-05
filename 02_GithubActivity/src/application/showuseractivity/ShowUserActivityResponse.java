package application.showuseractivity;

import domain.ActivitySummary;
import domain.ActivityType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * @param username   the login the activity belongs to
 * @param summaries  the rolled-up activity, newest first
 * @param servedFromCache whether the feed came from the cache
 * @param fetchedAt  when the underlying feed was read from GitHub
 * @param appliedFilter the activity type the feed was narrowed to, if any
 */
public record ShowUserActivityResponse(String username, List<ActivitySummary> summaries, boolean servedFromCache,
                                       Instant fetchedAt, ActivityType appliedFilter) {

    public ShowUserActivityResponse {
        summaries = List.copyOf(summaries);
    }

    public ShowUserActivityResponse(String username, List<ActivitySummary> summaries, boolean servedFromCache,
                                    Instant fetchedAt) {
        this(username, summaries, servedFromCache, fetchedAt, null);
    }

    public Optional<ActivityType> filter() {
        return Optional.ofNullable(appliedFilter);
    }
}
