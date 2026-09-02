package domain;

import java.time.Instant;

public final class Task {

    private final int id;
    private String description;
    private TaskStatus status;
    private final Instant createdAt;
    private Instant updatedAt;

    public Task(int id, String description, TaskStatus status, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.description = requireNonBlank(description);
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Task createNew(int id, String description, Instant now) {
        return new Task(id, description, TaskStatus.TODO, now, now);
    }

    public void updateDescription(String newDescription, Instant now) {
        this.description = requireNonBlank(newDescription);
        this.updatedAt = now;
    }

    public void changeStatus(TaskStatus newStatus, Instant now) {
        this.status = newStatus;
        this.updatedAt = now;
    }

    private static String requireNonBlank(String description) {
        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException("Task description must not be blank");
        }
        return description;
    }

    public int id() {
        return id;
    }

    public String description() {
        return description;
    }

    public TaskStatus status() {
        return status;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }
}
