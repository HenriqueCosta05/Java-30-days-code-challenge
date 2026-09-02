package application;

import domain.Task;
import domain.TaskNotFoundException;
import domain.TaskStatus;
import java.time.Instant;

public final class MarkTaskStatusUseCase {

    private final TaskRepository repository;

    public MarkTaskStatusUseCase(TaskRepository repository) {
        this.repository = repository;
    }

    public Task execute(int id, TaskStatus newStatus) {
        Task task = repository.findById(id).orElseThrow(() -> new TaskNotFoundException(id));
        task.changeStatus(newStatus, Instant.now());
        repository.save(task);
        return task;
    }
}
