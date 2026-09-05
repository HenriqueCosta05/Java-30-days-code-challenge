package infrastructure.github;

import application.showuseractivity.ActivityFetchFailedException;
import application.showuseractivity.GithubActivityGateway;
import application.showuseractivity.RateLimitedException;
import application.showuseractivity.UserNotFoundException;
import domain.ActivityEvent;
import domain.GithubUsername;
import infrastructure.json.JsonParseException;
import infrastructure.json.JsonParser;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

/**
 * Reads the public events feed of the GitHub REST API over HTTP.
 */
public final class HttpGithubActivityGateway implements GithubActivityGateway {

    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(15);

    private final HttpClient httpClient;
    private final String baseUrl;

    public HttpGithubActivityGateway(HttpClient httpClient, String baseUrl) {
        this.httpClient = httpClient;
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    @Override
    public List<ActivityEvent> fetchRecentActivity(GithubUsername username) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/users/" + username.value() + "/events"))
                .header("Accept", "application/vnd.github+json")
                .header("X-GitHub-Api-Version", "2022-11-28")
                .header("User-Agent", "github-activity-cli")
                .timeout(REQUEST_TIMEOUT)
                .GET()
                .build();

        HttpResponse<String> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException unreachable) {
            throw new ActivityFetchFailedException(
                    "Could not reach GitHub: " + unreachable.getMessage(), unreachable);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new ActivityFetchFailedException("The request to GitHub was interrupted.", interrupted);
        }

        return switch (response.statusCode()) {
            case 200 -> parse(response.body());
            case 404 -> throw new UserNotFoundException(username);
            case 403, 429 -> throw new RateLimitedException(
                    "GitHub rejected the request because of its rate limit. Try again later.");
            case 401 -> throw new ActivityFetchFailedException("GitHub rejected the request as unauthorized.");
            default -> throw new ActivityFetchFailedException(
                    "GitHub answered with an unexpected status: " + response.statusCode());
        };
    }

    private static List<ActivityEvent> parse(String body) {
        try {
            return GithubEventMapper.mapAll(JsonParser.parse(body));
        } catch (JsonParseException | IllegalArgumentException malformed) {
            throw new ActivityFetchFailedException(
                    "GitHub answered with a body this program could not read: " + malformed.getMessage(), malformed);
        }
    }
}
