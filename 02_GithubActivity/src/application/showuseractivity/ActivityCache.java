package application.showuseractivity;

import domain.ActivityEvent;
import domain.GithubUsername;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Port for remembering a previously fetched feed. Whether the remembered
 * feed is still good enough to use is decided by the use case, not here.
 */
public interface ActivityCache {

    Optional<CachedActivity> find(GithubUsername username);

    void store(GithubUsername username, List<ActivityEvent> events, Instant fetchedAt);
}
