package infrastructure.json;

import application.TaskRepository;
import domain.Task;
import domain.TaskStatus;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class JsonTaskRepository implements TaskRepository {

    private final Path storageFile;

    public JsonTaskRepository(Path storageFile) {
        this.storageFile = storageFile;
    }

    @Override
    public int nextId() {
        return loadAll().stream().mapToInt(Task::id).max().orElse(0) + 1;
    }

    @Override
    public void save(Task task) {
        List<Task> tasks = loadAll();
        tasks.removeIf(existing -> existing.id() == task.id());
        tasks.add(task);
        writeAll(tasks);
    }

    @Override
    public Optional<Task> findById(int id) {
        return loadAll().stream().filter(task -> task.id() == id).findFirst();
    }

    @Override
    public List<Task> findAll() {
        return loadAll();
    }

    @Override
    public boolean deleteById(int id) {
        List<Task> tasks = loadAll();
        boolean removed = tasks.removeIf(task -> task.id() == id);
        if (removed) {
            writeAll(tasks);
        }
        return removed;
    }

    @SuppressWarnings("unchecked")
    private List<Task> loadAll() {
        if (Files.notExists(storageFile)) {
            return new ArrayList<>();
        }
        try {
            String content = Files.readString(storageFile);
            if (content.isBlank()) {
                return new ArrayList<>();
            }
            List<Object> rawTasks = (List<Object>) JsonParser.parse(content);
            List<Task> tasks = new ArrayList<>();
            for (Object rawTask : rawTasks) {
                tasks.add(toTask((Map<String, Object>) rawTask));
            }
            return tasks;
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read tasks file: " + storageFile, e);
        }
    }

    private void writeAll(List<Task> tasks) {
        List<Map<String, Object>> raw = tasks.stream().map(this::toMap).toList();
        try {
            Files.writeString(storageFile, JsonWriter.write(raw));
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to write tasks file: " + storageFile, e);
        }
    }

    private Task toTask(Map<String, Object> raw) {
        return new Task(
                ((Double) raw.get("id")).intValue(),
                (String) raw.get("description"),
                TaskStatus.fromLabel((String) raw.get("status")),
                Instant.parse((String) raw.get("createdAt")),
                Instant.parse((String) raw.get("updatedAt")));
    }

    private Map<String, Object> toMap(Task task) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", task.id());
        map.put("description", task.description());
        map.put("status", task.status().label());
        map.put("createdAt", task.createdAt().toString());
        map.put("updatedAt", task.updatedAt().toString());
        return map;
    }
}
