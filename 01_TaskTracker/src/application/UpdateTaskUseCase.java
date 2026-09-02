package application;

import domain.Task;
import domain.TaskNotFoundException;
import java.time.Instant;

public final class UpdateTaskUseCase {

    private final TaskRepository repository;

    public UpdateTaskUseCase(TaskRepository repository) {
        this.repository = repository;
    }

    public Task execute(int id, String newDescription) {
        Task task = repository.findById(id).orElseThrow(() -> new TaskNotFoundException(id));
        task.updateDescription(newDescription, Instant.now());
        repository.save(task);
        return task;
    }
}
