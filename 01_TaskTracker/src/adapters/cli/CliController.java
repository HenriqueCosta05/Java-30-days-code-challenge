package adapters.cli;

import application.AddTaskUseCase;
import application.DeleteTaskUseCase;
import application.ListTasksUseCase;
import application.MarkTaskStatusUseCase;
import application.UpdateTaskUseCase;
import domain.Task;
import domain.TaskNotFoundException;
import domain.TaskStatus;
import java.io.PrintStream;
import java.util.List;
import java.util.Optional;

public final class CliController {

    private final AddTaskUseCase addTask;
    private final UpdateTaskUseCase updateTask;
    private final DeleteTaskUseCase deleteTask;
    private final MarkTaskStatusUseCase markTaskStatus;
    private final ListTasksUseCase listTasks;
    private final PrintStream out;
    private final PrintStream err;

    public CliController(
            AddTaskUseCase addTask,
            UpdateTaskUseCase updateTask,
            DeleteTaskUseCase deleteTask,
            MarkTaskStatusUseCase markTaskStatus,
            ListTasksUseCase listTasks,
            PrintStream out,
            PrintStream err) {
        this.addTask = addTask;
        this.updateTask = updateTask;
        this.deleteTask = deleteTask;
        this.markTaskStatus = markTaskStatus;
        this.listTasks = listTasks;
        this.out = out;
        this.err = err;
    }

    public int run(String[] args) {
        if (args.length == 0) {
            err.println("Usage: task-cli <command> [arguments]");
            return 1;
        }
        try {
            dispatch(args);
            return 0;
        } catch (IllegalArgumentException e) {
            err.println("Error: " + e.getMessage());
            return 1;
        } catch (TaskNotFoundException e) {
            err.println("Error: " + e.getMessage());
            return 1;
        }
    }

    private void dispatch(String[] args) {
        String command = args[0];
        switch (command) {
            case "add" -> handleAdd(args);
            case "update" -> handleUpdate(args);
            case "delete" -> handleDelete(args);
            case "mark-in-progress" -> handleMarkStatus(args, TaskStatus.IN_PROGRESS);
            case "mark-done" -> handleMarkStatus(args, TaskStatus.DONE);
            case "list" -> handleList(args);
            default -> throw new IllegalArgumentException("Unknown command: " + command);
        }
    }

    private void handleAdd(String[] args) {
        String description = requireArg(args, 1, "add \"<description>\"");
        Task task = addTask.execute(description);
        out.println("Task added successfully (ID: " + task.id() + ")");
    }

    private void handleUpdate(String[] args) {
        int id = requireIdArg(args, 1, "update <id> \"<description>\"");
        String description = requireArg(args, 2, "update <id> \"<description>\"");
        updateTask.execute(id, description);
        out.println("Task " + id + " updated successfully");
    }

    private void handleDelete(String[] args) {
        int id = requireIdArg(args, 1, "delete <id>");
        deleteTask.execute(id);
        out.println("Task " + id + " deleted successfully");
    }

    private void handleMarkStatus(String[] args, TaskStatus status) {
        int id = requireIdArg(args, 1, "mark-" + status.label() + " <id>");
        markTaskStatus.execute(id, status);
        out.println("Task " + id + " marked as " + status.label());
    }

    private void handleList(String[] args) {
        Optional<TaskStatus> filter = args.length > 1
                ? Optional.of(TaskStatus.fromLabel(args[1]))
                : Optional.empty();
        List<Task> tasks = listTasks.execute(filter);
        if (tasks.isEmpty()) {
            out.println("No tasks found");
            return;
        }
        for (Task task : tasks) {
            out.printf("[%d] %-12s %s%n", task.id(), task.status().label(), task.description());
        }
    }

    private String requireArg(String[] args, int index, String usage) {
        if (args.length <= index || args[index].isBlank()) {
            throw new IllegalArgumentException("Missing argument. Usage: task-cli " + usage);
        }
        return args[index];
    }

    private int requireIdArg(String[] args, int index, String usage) {
        String raw = requireArg(args, index, usage);
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid task id: " + raw);
        }
    }
}
