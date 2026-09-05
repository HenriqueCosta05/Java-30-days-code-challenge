import infrastructure.github.GithubFeedTests;
import testing.Assertions;

/**
 * Runs the suite. The core tests need nothing but the JDK.
 */
public final class TestRunner {

    private TestRunner() {
    }

    public static void main(String[] args) {
        CoreTests.run();
        AdapterTests.run();
        GithubFeedTests.run();
        CacheTests.run();
        System.exit(Assertions.report());
    }
}
