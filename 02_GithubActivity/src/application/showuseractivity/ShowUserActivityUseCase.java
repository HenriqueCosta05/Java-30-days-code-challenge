package application.showuseractivity;

import domain.ActivityEvent;
import domain.ActivityFeed;
import domain.ActivitySummary;
import domain.ActivityType;
import domain.GithubUsername;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Shows what a GitHub user has been up to lately: read the feed (from the
 * cache while it is still fresh), narrow it if asked, and roll it up.
 */
public final class ShowUserActivityUseCase {

    private final GithubActivityGateway gateway;
    private final ActivityCache cache;
    private final Clock clock;
    private final Duration cacheLifetime;

    public ShowUserActivityUseCase(GithubActivityGateway gateway, ActivityCache cache, Clock clock,
                                   Duration cacheLifetime) {
        this.gateway = gateway;
        this.cache = cache;
        this.clock = clock;
        this.cacheLifetime = cacheLifetime;
    }

    public ShowUserActivityResponse execute(ShowUserActivityRequest request) {
        GithubUsername username = GithubUsername.of(request.username());
        Instant now = clock.instant();

        Optional<CachedActivity> usable = request.refresh() ? Optional.empty() : freshCachedActivity(username, now);
        List<ActivityEvent> events;
        Instant fetchedAt;
        if (usable.isPresent()) {
            events = usable.get().events();
            fetchedAt = usable.get().fetchedAt();
        } else {
            events = gateway.fetchRecentActivity(username);
            fetchedAt = now;
            cache.store(username, events, now);
        }

        ActivityFeed feed = ActivityFeed.of(events);
        Optional<ActivityType> filter = request.activityTypeFilter();
        if (filter.isPresent()) {
            feed = feed.filterBy(filter.get());
        }

        List<ActivitySummary> summaries = truncate(feed.summarize(), request.maximumSummaries());
        return new ShowUserActivityResponse(
                username.value(), summaries, usable.isPresent(), fetchedAt, filter.orElse(null));
    }

    private Optional<CachedActivity> freshCachedActivity(GithubUsername username, Instant now) {
        return cache.find(username)
                .filter(cached -> Duration.between(cached.fetchedAt(), now).compareTo(cacheLifetime) < 0);
    }

    private static List<ActivitySummary> truncate(List<ActivitySummary> summaries, Optional<Integer> limit) {
        if (limit.isEmpty() || limit.get() >= summaries.size()) {
            return summaries;
        }
        return List.copyOf(summaries.subList(0, Math.max(0, limit.get())));
    }
}
