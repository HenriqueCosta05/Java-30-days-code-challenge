package application;

import domain.Task;
import domain.TaskStatus;
import java.util.List;
import java.util.Optional;

public final class ListTasksUseCase {

    private final TaskRepository repository;

    public ListTasksUseCase(TaskRepository repository) {
        this.repository = repository;
    }

    public List<Task> execute(Optional<TaskStatus> statusFilter) {
        List<Task> tasks = repository.findAll();
        if (statusFilter.isEmpty()) {
            return tasks;
        }
        return tasks.stream()
                .filter(task -> task.status() == statusFilter.get())
                .toList();
    }
}
