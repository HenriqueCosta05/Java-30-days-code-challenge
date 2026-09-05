package application.showuseractivity;

/**
 * The activity could not be retrieved. Raised by gateway implementations,
 * described in terms this use case understands rather than in terms of HTTP.
 */
public class ActivityFetchFailedException extends RuntimeException {

    public ActivityFetchFailedException(String message) {
        super(message);
    }

    public ActivityFetchFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}
