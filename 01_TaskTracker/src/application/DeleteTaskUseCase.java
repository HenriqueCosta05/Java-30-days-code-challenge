package application;

import domain.TaskNotFoundException;

public final class DeleteTaskUseCase {

    private final TaskRepository repository;

    public DeleteTaskUseCase(TaskRepository repository) {
        this.repository = repository;
    }

    public void execute(int id) {
        boolean deleted = repository.deleteById(id);
        if (!deleted) {
            throw new TaskNotFoundException(id);
        }
    }
}
