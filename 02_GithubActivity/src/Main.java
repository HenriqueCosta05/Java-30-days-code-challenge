import adapters.cli.ActivityTextPresenter;
import adapters.cli.GithubActivityCliController;
import application.showuseractivity.ActivityCache;
import application.showuseractivity.GithubActivityGateway;
import application.showuseractivity.ShowUserActivityUseCase;
import infrastructure.cache.FileSystemActivityCache;
import infrastructure.github.HttpGithubActivityGateway;
import java.net.http.HttpClient;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;

/**
 * Composition root: the only place that knows which details are plugged in.
 */
public final class Main {

    private static final String GITHUB_API = "https://api.github.com";
    private static final Duration CACHE_LIFETIME = Duration.ofMinutes(10);
    private static final Path CACHE_DIRECTORY = Path.of(".github-activity-cache");

    public static void main(String[] args) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();

        GithubActivityGateway gateway = new HttpGithubActivityGateway(httpClient, GITHUB_API);
        ActivityCache cache = new FileSystemActivityCache(CACHE_DIRECTORY);
        Clock clock = Clock.systemUTC();

        GithubActivityCliController controller = new GithubActivityCliController(
                new ShowUserActivityUseCase(gateway, cache, clock, CACHE_LIFETIME),
                new ActivityTextPresenter(),
                clock,
                System.out,
                System.err);

        System.exit(controller.run(args));
    }
}
