package application.showuseractivity;

import domain.ActivityEvent;
import domain.GithubUsername;
import java.util.List;

/**
 * Port for reading a user's recent public activity from GitHub.
 */
public interface GithubActivityGateway {

    /**
     * @throws UserNotFoundException if the user does not exist
     * @throws RateLimitedException if GitHub refused the request for quota reasons
     * @throws ActivityFetchFailedException if the activity could not be read
     */
    List<ActivityEvent> fetchRecentActivity(GithubUsername username);
}
