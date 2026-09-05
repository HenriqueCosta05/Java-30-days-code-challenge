package infrastructure.github;

import domain.ActivityEvent;
import domain.ActivityType;
import domain.RepositoryName;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Turns one entry of the GitHub events feed into a domain activity event.
 * Entries this application has nothing to say about are dropped rather than
 * failing the whole feed.
 */
final class GithubEventMapper {

    private GithubEventMapper() {
    }

    static List<ActivityEvent> mapAll(Object parsedFeed) {
        if (!(parsedFeed instanceof List<?> entries)) {
            throw new IllegalArgumentException("The GitHub events feed was expected to be an array.");
        }
        return entries.stream()
                .map(GithubEventMapper::map)
                .flatMap(Optional::stream)
                .toList();
    }

    private static Optional<ActivityEvent> map(Object entry) {
        if (!(entry instanceof Map<?, ?> event)) {
            return Optional.empty();
        }
        Optional<String> apiType = text(event, "type");
        Optional<String> repository = text(nested(event, "repo"), "name");
        Optional<Instant> occurredAt = text(event, "created_at").flatMap(GithubEventMapper::instant);
        if (apiType.isEmpty() || repository.isEmpty() || occurredAt.isEmpty()) {
            return Optional.empty();
        }

        Map<?, ?> payload = nested(event, "payload");
        String action = text(payload, "action").orElse("");
        Optional<ActivityType> type = activityType(apiType.get(), action);
        if (type.isEmpty()) {
            return Optional.empty();
        }

        int count = type.get() == ActivityType.PUSHED_COMMITS ? number(payload, "size").orElse(0) : 1;
        if (count < 1) {
            return Optional.empty();
        }

        try {
            return Optional.of(ActivityEvent.of(
                    type.get(), RepositoryName.of(repository.get()), count, occurredAt.get()));
        } catch (RuntimeException malformed) {
            return Optional.empty();
        }
    }

    private static Optional<ActivityType> activityType(String apiType, String action) {
        return switch (apiType) {
            case "PushEvent" -> Optional.of(ActivityType.PUSHED_COMMITS);
            case "IssuesEvent" -> switch (action) {
                case "opened" -> Optional.of(ActivityType.OPENED_ISSUE);
                case "closed" -> Optional.of(ActivityType.CLOSED_ISSUE);
                case "reopened" -> Optional.of(ActivityType.REOPENED_ISSUE);
                default -> Optional.<ActivityType>empty();
            };
            case "IssueCommentEvent" ->
                    action.equals("created") ? Optional.of(ActivityType.COMMENTED_ON_ISSUE) : Optional.empty();
            case "PullRequestEvent" -> switch (action) {
                case "opened", "reopened" -> Optional.of(ActivityType.OPENED_PULL_REQUEST);
                case "closed" -> Optional.of(ActivityType.CLOSED_PULL_REQUEST);
                default -> Optional.<ActivityType>empty();
            };
            case "PullRequestReviewEvent", "PullRequestReviewCommentEvent" ->
                    Optional.of(ActivityType.REVIEWED_PULL_REQUEST);
            case "WatchEvent" -> Optional.of(ActivityType.STARRED);
            case "ForkEvent" -> Optional.of(ActivityType.FORKED);
            case "CreateEvent" -> Optional.of(ActivityType.CREATED_BRANCH_OR_TAG);
            case "DeleteEvent" -> Optional.of(ActivityType.DELETED_BRANCH_OR_TAG);
            case "ReleaseEvent" ->
                    action.equals("published") ? Optional.of(ActivityType.PUBLISHED_RELEASE) : Optional.empty();
            case "PublicEvent" -> Optional.of(ActivityType.MADE_PUBLIC);
            case "MemberEvent" -> Optional.of(ActivityType.JOINED_REPOSITORY);
            default -> Optional.empty();
        };
    }

    private static Map<?, ?> nested(Map<?, ?> source, String name) {
        Object value = source.get(name);
        return value instanceof Map<?, ?> map ? map : Map.of();
    }

    private static Optional<String> text(Map<?, ?> source, String name) {
        Object value = source.get(name);
        return value instanceof String text && !text.isBlank() ? Optional.of(text) : Optional.empty();
    }

    private static Optional<Integer> number(Map<?, ?> source, String name) {
        Object value = source.get(name);
        return value instanceof Number quantity ? Optional.of(quantity.intValue()) : Optional.empty();
    }

    private static Optional<Instant> instant(String text) {
        try {
            return Optional.of(Instant.parse(text));
        } catch (DateTimeParseException notATimestamp) {
            return Optional.empty();
        }
    }
}
