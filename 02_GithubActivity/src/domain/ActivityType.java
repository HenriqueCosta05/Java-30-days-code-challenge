package domain;

/**
 * What a user actually did, expressed in the language of the domain rather
 * than in the vocabulary of the GitHub events API.
 */
public enum ActivityType {

    PUSHED_COMMITS(true),
    OPENED_ISSUE(false),
    CLOSED_ISSUE(false),
    REOPENED_ISSUE(false),
    COMMENTED_ON_ISSUE(false),
    OPENED_PULL_REQUEST(false),
    CLOSED_PULL_REQUEST(false),
    REVIEWED_PULL_REQUEST(false),
    STARRED(false),
    FORKED(false),
    CREATED_BRANCH_OR_TAG(false),
    DELETED_BRANCH_OR_TAG(false),
    PUBLISHED_RELEASE(false),
    MADE_PUBLIC(false),
    JOINED_REPOSITORY(false);

    private final boolean countable;

    ActivityType(boolean countable) {
        this.countable = countable;
    }

    /**
     * Countable activities carry a meaningful quantity (commits pushed);
     * every other activity happened exactly once per event.
     */
    public boolean isCountable() {
        return countable;
    }
}
