package adapters.cli;

import application.showuseractivity.ShowUserActivityResponse;
import domain.ActivitySummary;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Renders activity as the plain bullet list the terminal shows.
 */
public final class ActivityTextPresenter implements ActivityPresenter {

    @Override
    public List<String> present(ShowUserActivityResponse response, Instant now) {
        List<String> lines = new ArrayList<>();
        lines.add(header(response, now));
        if (response.summaries().isEmpty()) {
            lines.add(response.filter().isPresent()
                    ? "- No recent public activity of that kind."
                    : "- No recent public activity.");
            return List.copyOf(lines);
        }
        for (ActivitySummary summary : response.summaries()) {
            lines.add("- " + describe(summary));
        }
        return List.copyOf(lines);
    }

    private static String header(ShowUserActivityResponse response, Instant now) {
        String origin = response.servedFromCache()
                ? " (cached " + humanize(Duration.between(response.fetchedAt(), now)) + ")"
                : "";
        return "Recent activity for " + response.username() + origin + ":";
    }

    private static String describe(ActivitySummary summary) {
        String repository = summary.repository().value();
        int count = summary.count();
        return switch (summary.type()) {
            case PUSHED_COMMITS -> "Pushed " + count + plural(count, " commit", " commits") + " to " + repository;
            case OPENED_ISSUE -> opened(count, "issue") + " in " + repository;
            case CLOSED_ISSUE -> "Closed " + count + plural(count, " issue", " issues") + " in " + repository;
            case REOPENED_ISSUE -> "Reopened " + count + plural(count, " issue", " issues") + " in " + repository;
            case COMMENTED_ON_ISSUE ->
                    "Commented " + count + plural(count, " time", " times") + " on issues in " + repository;
            case OPENED_PULL_REQUEST -> opened(count, "pull request") + " in " + repository;
            case CLOSED_PULL_REQUEST ->
                    "Closed " + count + plural(count, " pull request", " pull requests") + " in " + repository;
            case REVIEWED_PULL_REQUEST ->
                    "Reviewed " + count + plural(count, " pull request", " pull requests") + " in " + repository;
            case STARRED -> "Starred " + repository;
            case FORKED -> "Forked " + repository;
            case CREATED_BRANCH_OR_TAG ->
                    "Created " + count + plural(count, " branch or tag", " branches or tags") + " in " + repository;
            case DELETED_BRANCH_OR_TAG ->
                    "Deleted " + count + plural(count, " branch or tag", " branches or tags") + " in " + repository;
            case PUBLISHED_RELEASE ->
                    "Published " + count + plural(count, " release", " releases") + " in " + repository;
            case MADE_PUBLIC -> "Made " + repository + " public";
            case JOINED_REPOSITORY -> "Joined " + repository + " as a collaborator";
        };
    }

    private static String opened(int count, String noun) {
        if (count == 1) {
            return "Opened a new " + noun;
        }
        return "Opened " + count + " new " + noun + "s";
    }

    private static String plural(int count, String singular, String plural) {
        return count == 1 ? singular : plural;
    }

    private static String humanize(Duration age) {
        long seconds = Math.max(0, age.getSeconds());
        if (seconds < 60) {
            return "just now";
        }
        long minutes = seconds / 60;
        if (minutes < 60) {
            return minutes + (minutes == 1 ? " minute ago" : " minutes ago");
        }
        long hours = minutes / 60;
        if (hours < 24) {
            return hours + (hours == 1 ? " hour ago" : " hours ago");
        }
        long days = hours / 24;
        return days + (days == 1 ? " day ago" : " days ago");
    }
}
