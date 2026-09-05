package domain;

public final class InvalidRepositoryNameException extends RuntimeException {

    public InvalidRepositoryNameException(String message) {
        super(message);
    }
}
