package domain;

import java.util.Objects;

/**
 * The "owner/repository" pair an activity event belongs to.
 */
public final class RepositoryName {

    private final String value;

    private RepositoryName(String value) {
        this.value = value;
    }

    public static RepositoryName of(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            throw new InvalidRepositoryNameException("Repository name must not be empty.");
        }
        String candidate = rawValue.trim();
        int separator = candidate.indexOf('/');
        if (separator <= 0 || separator != candidate.lastIndexOf('/') || separator == candidate.length() - 1) {
            throw new InvalidRepositoryNameException(
                    "Repository name must have the form owner/repository: " + candidate);
        }
        return new RepositoryName(candidate);
    }

    public String value() {
        return value;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        return other instanceof RepositoryName that && value.equals(that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
