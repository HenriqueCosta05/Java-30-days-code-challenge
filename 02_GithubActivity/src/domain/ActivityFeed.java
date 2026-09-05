package domain;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A user's recent activity, newest first, and the rules for reading it.
 */
public final class ActivityFeed {

    private final List<ActivityEvent> events;

    private ActivityFeed(List<ActivityEvent> events) {
        this.events = events;
    }

    public static ActivityFeed of(List<ActivityEvent> events) {
        List<ActivityEvent> ordered = new ArrayList<>(events);
        ordered.sort(Comparator.comparing(ActivityEvent::occurredAt).reversed());
        return new ActivityFeed(List.copyOf(ordered));
    }

    public boolean isEmpty() {
        return events.isEmpty();
    }

    public List<ActivityEvent> events() {
        return events;
    }

    public ActivityFeed filterBy(ActivityType type) {
        List<ActivityEvent> matching = new ArrayList<>();
        for (ActivityEvent event : events) {
            if (event.type() == type) {
                matching.add(event);
            }
        }
        return new ActivityFeed(List.copyOf(matching));
    }

    /**
     * Rolls the feed up into one line of story per (activity, repository)
     * pair, keeping the order in which those pairs were most recently seen.
     */
    public List<ActivitySummary> summarize() {
        Map<String, ActivitySummary> rolledUp = new LinkedHashMap<>();
        for (ActivityEvent event : events) {
            String key = event.type().name() + '@' + event.repository().value();
            ActivitySummary running = rolledUp.get(key);
            if (running == null) {
                rolledUp.put(key, new ActivitySummary(
                        event.type(), event.repository(), event.count(), event.occurredAt()));
                continue;
            }
            rolledUp.put(key, new ActivitySummary(
                    running.type(),
                    running.repository(),
                    running.count() + event.count(),
                    running.lastOccurredAt()));
        }
        return List.copyOf(rolledUp.values());
    }
}
