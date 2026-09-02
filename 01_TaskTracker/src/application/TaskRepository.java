package application;

import domain.Task;
import java.util.List;
import java.util.Optional;

public interface TaskRepository {

    int nextId();

    void save(Task task);

    Optional<Task> findById(int id);

    List<Task> findAll();

    boolean deleteById(int id);
}
