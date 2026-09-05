package infrastructure.json;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A small recursive descent JSON reader, enough for the GitHub events feed.
 * Objects become maps, arrays become lists, numbers become doubles.
 */
public final class JsonParser {

    private final String source;
    private int position;

    private JsonParser(String source) {
        this.source = source;
    }

    public static Object parse(String json) {
        if (json == null) {
            throw new JsonParseException("There is no JSON to read.");
        }
        JsonParser parser = new JsonParser(json);
        parser.skipWhitespace();
        Object value = parser.readValue();
        parser.skipWhitespace();
        if (parser.position < parser.source.length()) {
            throw new JsonParseException("Unexpected content at position " + parser.position);
        }
        return value;
    }

    private Object readValue() {
        char current = peek();
        return switch (current) {
            case '{' -> readObject();
            case '[' -> readArray();
            case '"' -> readString();
            case 't' -> readLiteral("true", Boolean.TRUE);
            case 'f' -> readLiteral("false", Boolean.FALSE);
            case 'n' -> readLiteral("null", null);
            default -> readNumber();
        };
    }

    private Map<String, Object> readObject() {
        expect('{');
        Map<String, Object> members = new LinkedHashMap<>();
        skipWhitespace();
        if (peek() == '}') {
            position++;
            return members;
        }
        while (true) {
            skipWhitespace();
            String name = readString();
            skipWhitespace();
            expect(':');
            skipWhitespace();
            members.put(name, readValue());
            skipWhitespace();
            char separator = next();
            if (separator == '}') {
                return members;
            }
            if (separator != ',') {
                throw new JsonParseException("Expected ',' or '}' at position " + (position - 1));
            }
        }
    }

    private List<Object> readArray() {
        expect('[');
        List<Object> elements = new ArrayList<>();
        skipWhitespace();
        if (peek() == ']') {
            position++;
            return elements;
        }
        while (true) {
            skipWhitespace();
            elements.add(readValue());
            skipWhitespace();
            char separator = next();
            if (separator == ']') {
                return elements;
            }
            if (separator != ',') {
                throw new JsonParseException("Expected ',' or ']' at position " + (position - 1));
            }
        }
    }

    private String readString() {
        expect('"');
        StringBuilder text = new StringBuilder();
        while (true) {
            char current = next();
            if (current == '"') {
                return text.toString();
            }
            if (current != '\\') {
                text.append(current);
                continue;
            }
            char escaped = next();
            switch (escaped) {
                case '"', '\\', '/' -> text.append(escaped);
                case 'b' -> text.append('\b');
                case 'f' -> text.append('\f');
                case 'n' -> text.append('\n');
                case 'r' -> text.append('\r');
                case 't' -> text.append('\t');
                case 'u' -> {
                    if (position + 4 > source.length()) {
                        throw new JsonParseException("Truncated unicode escape at position " + position);
                    }
                    String code = source.substring(position, position + 4);
                    position += 4;
                    try {
                        text.append((char) Integer.parseInt(code, 16));
                    } catch (NumberFormatException notHex) {
                        throw new JsonParseException("Invalid unicode escape: \\u" + code);
                    }
                }
                default -> throw new JsonParseException("Unsupported escape: \\" + escaped);
            }
        }
    }

    private Double readNumber() {
        int start = position;
        while (position < source.length() && "+-.eE0123456789".indexOf(source.charAt(position)) >= 0) {
            position++;
        }
        String literal = source.substring(start, position);
        try {
            return Double.valueOf(literal);
        } catch (NumberFormatException notANumber) {
            throw new JsonParseException("Invalid number at position " + start + ": " + literal);
        }
    }

    private Object readLiteral(String literal, Object value) {
        if (!source.startsWith(literal, position)) {
            throw new JsonParseException("Expected " + literal + " at position " + position);
        }
        position += literal.length();
        return value;
    }

    private void skipWhitespace() {
        while (position < source.length() && Character.isWhitespace(source.charAt(position))) {
            position++;
        }
    }

    private char peek() {
        if (position >= source.length()) {
            throw new JsonParseException("Unexpected end of JSON.");
        }
        return source.charAt(position);
    }

    private char next() {
        char current = peek();
        position++;
        return current;
    }

    private void expect(char expected) {
        char current = next();
        if (current != expected) {
            throw new JsonParseException("Expected '" + expected + "' at position " + (position - 1));
        }
    }
}
