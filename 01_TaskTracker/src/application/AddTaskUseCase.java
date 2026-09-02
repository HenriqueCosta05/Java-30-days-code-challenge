package application;

import domain.Task;
import java.time.Instant;

public final class AddTaskUseCase {

    private final TaskRepository repository;

    public AddTaskUseCase(TaskRepository repository) {
        this.repository = repository;
    }

    public Task execute(String description) {
        Task task = Task.createNew(repository.nextId(), description, Instant.now());
        repository.save(task);
        return task;
    }
}
