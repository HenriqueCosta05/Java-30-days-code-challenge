package application.showuseractivity;

import domain.ActivityEvent;
import java.time.Instant;
import java.util.List;

public record CachedActivity(List<ActivityEvent> events, Instant fetchedAt) {

    public CachedActivity {
        events = List.copyOf(events);
    }
}
