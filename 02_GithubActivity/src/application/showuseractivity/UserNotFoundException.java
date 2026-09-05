package application.showuseractivity;

import domain.GithubUsername;

public final class UserNotFoundException extends ActivityFetchFailedException {

    public UserNotFoundException(GithubUsername username) {
        super("No GitHub user named '" + username.value() + "' was found.");
    }
}
