package testing;

import java.util.ArrayList;
import java.util.List;

/**
 * A pocket sized test harness, so the core can be tested without a
 * testing framework on the classpath.
 */
public final class Assertions {

    private static final List<String> FAILURES = new ArrayList<>();
    private static int checks;

    private Assertions() {
    }

    public static void check(String name, Runnable test) {
        checks++;
        try {
            test.run();
            System.out.println("  ok   " + name);
        } catch (Throwable failure) {
            FAILURES.add(name + ": " + failure.getMessage());
            System.out.println("  FAIL " + name + " -> " + failure.getMessage());
        }
    }

    public static void assertEquals(Object expected, Object actual) {
        if (expected == null ? actual != null : !expected.equals(actual)) {
            throw new AssertionError("expected <" + expected + "> but was <" + actual + ">");
        }
    }

    public static void assertTrue(String message, boolean condition) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    public static void assertThrows(Class<? extends Throwable> expected, Runnable action) {
        try {
            action.run();
        } catch (Throwable thrown) {
            if (expected.isInstance(thrown)) {
                return;
            }
            throw new AssertionError("expected " + expected.getSimpleName() + " but was " + thrown);
        }
        throw new AssertionError("expected " + expected.getSimpleName() + " but nothing was thrown");
    }

    public static void section(String name) {
        System.out.println(name);
    }

    public static int report() {
        System.out.println();
        if (FAILURES.isEmpty()) {
            System.out.println(checks + " checks passed.");
            return 0;
        }
        System.out.println(FAILURES.size() + " of " + checks + " checks failed:");
        FAILURES.forEach(failure -> System.out.println("  - " + failure));
        return 1;
    }
}
