import adapters.cli.CliController;
import application.AddTaskUseCase;
import application.DeleteTaskUseCase;
import application.ListTasksUseCase;
import application.MarkTaskStatusUseCase;
import application.TaskRepository;
import application.UpdateTaskUseCase;
import infrastructure.json.JsonTaskRepository;
import java.nio.file.Path;

public final class Main {

    public static void main(String[] args) {
        TaskRepository repository = new JsonTaskRepository(Path.of("tasks.json"));

        CliController controller = new CliController(
                new AddTaskUseCase(repository),
                new UpdateTaskUseCase(repository),
                new DeleteTaskUseCase(repository),
                new MarkTaskStatusUseCase(repository),
                new ListTasksUseCase(repository),
                System.out,
                System.err);

        int exitCode = controller.run(args);
        System.exit(exitCode);
    }
}
