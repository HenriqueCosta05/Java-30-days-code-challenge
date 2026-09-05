package application.showuseractivity;

import domain.ActivityType;
import java.util.Optional;

/**
 * @param username     the login to look up, still unvalidated
 * @param activityType show only this kind of activity, when present
 * @param limit        keep at most this many summaries, when present
 * @param refresh      ignore any cached feed and go to GitHub
 */
public record ShowUserActivityRequest(String username, ActivityType activityType, Integer limit, boolean refresh) {

    public static ShowUserActivityRequest forUser(String username) {
        return new ShowUserActivityRequest(username, null, null, false);
    }

    public Optional<ActivityType> activityTypeFilter() {
        return Optional.ofNullable(activityType);
    }

    public Optional<Integer> maximumSummaries() {
        return Optional.ofNullable(limit);
    }
}
