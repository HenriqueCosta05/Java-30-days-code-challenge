package infrastructure.github;

import static testing.Assertions.assertEquals;
import static testing.Assertions.assertThrows;
import static testing.Assertions.assertTrue;
import static testing.Assertions.check;
import static testing.Assertions.section;

import domain.ActivityEvent;
import domain.ActivityType;
import infrastructure.json.JsonParseException;
import infrastructure.json.JsonParser;
import java.util.List;
import java.util.Map;

/**
 * The JSON reader and the mapping from the GitHub events feed onto the
 * domain. Lives in the gateway package so it can see the mapper.
 */
public final class GithubFeedTests {

    private static final String QUOTE = String.valueOf((char) 34);
    private static final String BACKSLASH = String.valueOf((char) 92);

    private GithubFeedTests() {
    }

    public static void run() {
        section("Infrastructure: JSON reader");
        check("reads an object", () -> {
            Object parsed = JsonParser.parse(json("{'name': 'value'}"));
            assertEquals("value", ((Map<?, ?>) parsed).get("name"));
        });
        check("reads nested arrays and numbers", () -> {
            Object parsed = JsonParser.parse(json("{'sizes': [1, 2, 3]}"));
            List<?> sizes = (List<?>) ((Map<?, ?>) parsed).get("sizes");
            assertEquals(3, sizes.size());
            assertEquals(2, ((Number) sizes.get(1)).intValue());
        });
        check("reads true, false and null", () -> {
            List<?> parsed = (List<?>) JsonParser.parse(json("[true, false, null]"));
            assertEquals(Boolean.TRUE, parsed.get(0));
            assertEquals(Boolean.FALSE, parsed.get(1));
            assertEquals(null, parsed.get(2));
        });
        check("reads an escaped quote", () -> {
            String source = "[" + QUOTE + "a" + BACKSLASH + QUOTE + "b" + QUOTE + "]";
            assertEquals("a" + QUOTE + "b", ((List<?>) JsonParser.parse(source)).get(0));
        });
        check("reads a unicode escape", () -> {
            String source = "[" + QUOTE + BACKSLASH + "u0041" + QUOTE + "]";
            assertEquals("A", ((List<?>) JsonParser.parse(source)).get(0));
        });
        check("refuses trailing rubbish", () ->
                assertThrows(JsonParseException.class, () -> JsonParser.parse(json("{} oops"))));
        check("refuses a truncated document", () ->
                assertThrows(JsonParseException.class, () -> JsonParser.parse(json("{'a': "))));

        section("Infrastructure: GitHub event mapping");
        check("maps a push to its commit count", () -> {
            List<ActivityEvent> events = map("[{'type': 'PushEvent', 'repo': {'name': 'owner/repo'},"
                    + " 'payload': {'size': 3}, 'created_at': '2026-09-05T10:00:00Z'}]");
            assertEquals(1, events.size());
            assertEquals(ActivityType.PUSHED_COMMITS, events.get(0).type());
            assertEquals(3, events.get(0).count());
        });
        check("maps a watch event to a star", () -> {
            List<ActivityEvent> events = map("[{'type': 'WatchEvent', 'repo': {'name': 'owner/repo'},"
                    + " 'payload': {'action': 'started'}, 'created_at': '2026-09-05T10:00:00Z'}]");
            assertEquals(ActivityType.STARRED, events.get(0).type());
        });
        check("maps an opened issue", () -> {
            List<ActivityEvent> events = map("[{'type': 'IssuesEvent', 'repo': {'name': 'owner/repo'},"
                    + " 'payload': {'action': 'opened'}, 'created_at': '2026-09-05T10:00:00Z'}]");
            assertEquals(ActivityType.OPENED_ISSUE, events.get(0).type());
        });
        check("drops an issue action it has nothing to say about", () ->
                assertTrue("labelled issues are skipped", map("[{'type': 'IssuesEvent',"
                        + " 'repo': {'name': 'owner/repo'}, 'payload': {'action': 'labeled'},"
                        + " 'created_at': '2026-09-05T10:00:00Z'}]").isEmpty()));
        check("drops an unknown event type", () ->
                assertTrue("unknown events are skipped", map("[{'type': 'GollumEvent',"
                        + " 'repo': {'name': 'owner/repo'}, 'payload': {},"
                        + " 'created_at': '2026-09-05T10:00:00Z'}]").isEmpty()));
        check("drops a push with no commits", () ->
                assertTrue("empty pushes are skipped", map("[{'type': 'PushEvent',"
                        + " 'repo': {'name': 'owner/repo'}, 'payload': {'size': 0},"
                        + " 'created_at': '2026-09-05T10:00:00Z'}]").isEmpty()));
        check("drops an entry with a malformed repository", () ->
                assertTrue("malformed entries are skipped", map("[{'type': 'WatchEvent',"
                        + " 'repo': {'name': 'no-owner'}, 'payload': {},"
                        + " 'created_at': '2026-09-05T10:00:00Z'}]").isEmpty()));
        check("keeps the good entries of a mixed feed", () -> {
            List<ActivityEvent> events = map("[{'type': 'GollumEvent', 'repo': {'name': 'owner/repo'},"
                    + " 'payload': {}, 'created_at': '2026-09-05T10:00:00Z'},"
                    + " {'type': 'ForkEvent', 'repo': {'name': 'owner/repo'}, 'payload': {},"
                    + " 'created_at': '2026-09-05T11:00:00Z'}]");
            assertEquals(1, events.size());
            assertEquals(ActivityType.FORKED, events.get(0).type());
        });
    }

    private static List<ActivityEvent> map(String feedWithSingleQuotes) {
        return GithubEventMapper.mapAll(JsonParser.parse(json(feedWithSingleQuotes)));
    }

    private static String json(String withSingleQuotes) {
        return withSingleQuotes.replace("'", QUOTE);
    }
}
