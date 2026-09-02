package infrastructure.json;

import java.util.List;
import java.util.Map;

/**
 * Minimal JSON serializer for the object/array/string/number shapes used by
 * this project. Not a general-purpose library; kept intentionally small.
 */
public final class JsonWriter {

    private JsonWriter() {
    }

    public static String write(Object value) {
        StringBuilder builder = new StringBuilder();
        writeValue(value, builder);
        return builder.toString();
    }

    @SuppressWarnings("unchecked")
    private static void writeValue(Object value, StringBuilder builder) {
        switch (value) {
            case null -> builder.append("null");
            case String s -> writeString(s, builder);
            case Number n -> builder.append(n);
            case Boolean b -> builder.append(b);
            case Map<?, ?> map -> writeObject((Map<String, Object>) map, builder);
            case List<?> list -> writeArray(list, builder);
            default -> throw new IllegalArgumentException("Unsupported JSON value type: " + value.getClass());
        }
    }

    private static void writeObject(Map<String, Object> map, StringBuilder builder) {
        builder.append('{');
        boolean first = true;
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (!first) {
                builder.append(',');
            }
            first = false;
            writeString(entry.getKey(), builder);
            builder.append(':');
            writeValue(entry.getValue(), builder);
        }
        builder.append('}');
    }

    private static void writeArray(List<?> list, StringBuilder builder) {
        builder.append('[');
        boolean first = true;
        for (Object item : list) {
            if (!first) {
                builder.append(',');
            }
            first = false;
            writeValue(item, builder);
        }
        builder.append(']');
    }

    private static void writeString(String value, StringBuilder builder) {
        builder.append('"');
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"' -> builder.append("\\\"");
                case '\\' -> builder.append("\\\\");
                case '\n' -> builder.append("\\n");
                case '\t' -> builder.append("\\t");
                case '\r' -> builder.append("\\r");
                default -> builder.append(c);
            }
        }
        builder.append('"');
    }
}
