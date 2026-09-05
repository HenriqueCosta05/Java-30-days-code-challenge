package adapters.cli;

import domain.ActivityType;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Translates the --type argument into the domain vocabulary. The command
 * line spellings live here so the domain never learns them.
 */
final class ActivityTypeArgument {

    private static final Map<String, ActivityType> BY_ARGUMENT = new LinkedHashMap<>();

    static {
        BY_ARGUMENT.put("push", ActivityType.PUSHED_COMMITS);
        BY_ARGUMENT.put("issue-opened", ActivityType.OPENED_ISSUE);
        BY_ARGUMENT.put("issue-closed", ActivityType.CLOSED_ISSUE);
        BY_ARGUMENT.put("issue-reopened", ActivityType.REOPENED_ISSUE);
        BY_ARGUMENT.put("issue-comment", ActivityType.COMMENTED_ON_ISSUE);
        BY_ARGUMENT.put("pr-opened", ActivityType.OPENED_PULL_REQUEST);
        BY_ARGUMENT.put("pr-closed", ActivityType.CLOSED_PULL_REQUEST);
        BY_ARGUMENT.put("pr-review", ActivityType.REVIEWED_PULL_REQUEST);
        BY_ARGUMENT.put("star", ActivityType.STARRED);
        BY_ARGUMENT.put("fork", ActivityType.FORKED);
        BY_ARGUMENT.put("create", ActivityType.CREATED_BRANCH_OR_TAG);
        BY_ARGUMENT.put("delete", ActivityType.DELETED_BRANCH_OR_TAG);
        BY_ARGUMENT.put("release", ActivityType.PUBLISHED_RELEASE);
        BY_ARGUMENT.put("public", ActivityType.MADE_PUBLIC);
        BY_ARGUMENT.put("member", ActivityType.JOINED_REPOSITORY);
    }

    private ActivityTypeArgument() {
    }

    static Optional<ActivityType> parse(String argument) {
        return Optional.ofNullable(BY_ARGUMENT.get(argument.toLowerCase()));
    }

    static String supportedValues() {
        return String.join(", ", BY_ARGUMENT.keySet());
    }
}
