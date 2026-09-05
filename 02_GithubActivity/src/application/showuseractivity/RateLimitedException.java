package application.showuseractivity;

public final class RateLimitedException extends ActivityFetchFailedException {

    public RateLimitedException(String message) {
        super(message);
    }
}
