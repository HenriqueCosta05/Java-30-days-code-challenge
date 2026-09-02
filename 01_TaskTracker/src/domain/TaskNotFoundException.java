package domain;

public final class TaskNotFoundException extends RuntimeException {

    public TaskNotFoundException(int id) {
        super("Task not found: " + id);
    }
}
