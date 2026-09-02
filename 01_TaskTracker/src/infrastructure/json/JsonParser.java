package infrastructure.json;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Minimal recursive-descent JSON parser covering the subset needed by this
 * project (objects, arrays, strings, numbers, booleans, null). Not a
 * general-purpose library; kept intentionally small.
 */
public final class JsonParser {

    private final String source;
    private int position;

    private JsonParser(String source) {
        this.source = source;
    }

    public static Object parse(String source) {
        JsonParser parser = new JsonParser(source);
        parser.skipWhitespace();
        Object value = parser.parseValue();
        parser.skipWhitespace();
        return value;
    }

    private Object parseValue() {
        char current = peek();
        return switch (current) {
            case '{' -> parseObject();
            case '[' -> parseArray();
            case '"' -> parseString();
            case 't', 'f' -> parseBoolean();
            case 'n' -> parseNull();
            default -> parseNumber();
        };
    }

    private Map<String, Object> parseObject() {
        Map<String, Object> result = new LinkedHashMap<>();
        expect('{');
        skipWhitespace();
        if (peek() == '}') {
            position++;
            return result;
        }
        while (true) {
            skipWhitespace();
            String key = parseString();
            skipWhitespace();
            expect(':');
            skipWhitespace();
            result.put(key, parseValue());
            skipWhitespace();
            char next = source.charAt(position++);
            if (next == '}') {
                break;
            }
            if (next != ',') {
                throw new IllegalArgumentException("Malformed JSON object at position " + position);
            }
        }
        return result;
    }

    private List<Object> parseArray() {
        List<Object> result = new ArrayList<>();
        expect('[');
        skipWhitespace();
        if (peek() == ']') {
            position++;
            return result;
        }
        while (true) {
            skipWhitespace();
            result.add(parseValue());
            skipWhitespace();
            char next = source.charAt(position++);
            if (next == ']') {
                break;
            }
            if (next != ',') {
                throw new IllegalArgumentException("Malformed JSON array at position " + position);
            }
        }
        return result;
    }

    private String parseString() {
        expect('"');
        StringBuilder builder = new StringBuilder();
        while (true) {
            char current = source.charAt(position++);
            if (current == '"') {
                break;
            }
            if (current == '\\') {
                char escaped = source.charAt(position++);
                builder.append(switch (escaped) {
                    case '"' -> '"';
                    case '\\' -> '\\';
                    case '/' -> '/';
                    case 'n' -> '\n';
                    case 't' -> '\t';
                    case 'r' -> '\r';
                    case 'b' -> '\b';
                    case 'f' -> '\f';
                    case 'u' -> parseUnicodeEscape();
                    default -> throw new IllegalArgumentException("Unknown escape: \\" + escaped);
                });
            } else {
                builder.append(current);
            }
        }
        return builder.toString();
    }

    private char parseUnicodeEscape() {
        String hex = source.substring(position, position + 4);
        position += 4;
        return (char) Integer.parseInt(hex, 16);
    }

    private Double parseNumber() {
        int start = position;
        while (position < source.length() && isNumberChar(source.charAt(position))) {
            position++;
        }
        return Double.parseDouble(source.substring(start, position));
    }

    private boolean isNumberChar(char c) {
        return Character.isDigit(c) || c == '-' || c == '+' || c == '.' || c == 'e' || c == 'E';
    }

    private Boolean parseBoolean() {
        if (source.startsWith("true", position)) {
            position += 4;
            return Boolean.TRUE;
        }
        if (source.startsWith("false", position)) {
            position += 5;
            return Boolean.FALSE;
        }
        throw new IllegalArgumentException("Malformed boolean at position " + position);
    }

    private Object parseNull() {
        if (source.startsWith("null", position)) {
            position += 4;
            return null;
        }
        throw new IllegalArgumentException("Malformed null at position " + position);
    }

    private void expect(char expected) {
        char actual = source.charAt(position++);
        if (actual != expected) {
            throw new IllegalArgumentException("Expected '" + expected + "' but found '" + actual + "' at position " + (position - 1));
        }
    }

    private char peek() {
        return source.charAt(position);
    }

    private void skipWhitespace() {
        while (position < source.length() && Character.isWhitespace(source.charAt(position))) {
            position++;
        }
    }
}
