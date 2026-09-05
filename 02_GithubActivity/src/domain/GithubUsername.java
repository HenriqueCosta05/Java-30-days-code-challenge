package domain;

import java.util.Objects;

/**
 * A GitHub login, guarding the naming rules GitHub itself enforces:
 * 1 to 39 characters, alphanumerics and single hyphens, never starting
 * or ending with a hyphen.
 */
public final class GithubUsername {

    private static final int MAX_LENGTH = 39;

    private final String value;

    private GithubUsername(String value) {
        this.value = value;
    }

    public static GithubUsername of(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            throw new InvalidUsernameException("Username must not be empty.");
        }
        String candidate = rawValue.trim();
        if (candidate.length() > MAX_LENGTH) {
            throw new InvalidUsernameException(
                    "Username must be at most " + MAX_LENGTH + " characters: " + candidate);
        }
        if (candidate.startsWith("-") || candidate.endsWith("-")) {
            throw new InvalidUsernameException("Username must not start or end with a hyphen: " + candidate);
        }
        if (candidate.contains("--")) {
            throw new InvalidUsernameException("Username must not contain consecutive hyphens: " + candidate);
        }
        for (int i = 0; i < candidate.length(); i++) {
            char character = candidate.charAt(i);
            boolean allowed = character == '-'
                    || (character >= '0' && character <= '9')
                    || (character >= 'a' && character <= 'z')
                    || (character >= 'A' && character <= 'Z');
            if (!allowed) {
                throw new InvalidUsernameException(
                        "Username may only contain letters, digits and hyphens: " + candidate);
            }
        }
        return new GithubUsername(candidate);
    }

    public String value() {
        return value;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        return other instanceof GithubUsername that && value.equalsIgnoreCase(that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value.toLowerCase());
    }

    @Override
    public String toString() {
        return value;
    }
}
