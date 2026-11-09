package app.todo.testsupport;

/** Tiny JSON writer for request bodies, so tests do not depend on a JSON library's package or version. */
public final class Json {

    /** A value written as is (for example a nested object or a number). */
    public record Raw(String json) {}

    /** The JSON literal null (a Java null in {@link #object} is not allowed; use this). */
    public static final Raw NULL = new Raw("null");

    private Json() {}

    public static Raw raw(String json) {
        return new Raw(json);
    }

    /** {@code object("a", 1, "b", "x")} gives {@code {"a":1,"b":"x"}}. */
    public static String object(Object... keyValues) {
        if (keyValues.length % 2 != 0) {
            throw new IllegalArgumentException("object() takes key/value pairs");
        }
        StringBuilder sb = new StringBuilder("{");
        for (int i = 0; i < keyValues.length; i += 2) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(quote((String) keyValues[i])).append(':').append(value(keyValues[i + 1]));
        }
        return sb.append('}').toString();
    }

    public static String value(Object v) {
        if (v == null) {
            throw new IllegalArgumentException("use Json.NULL for a JSON null, and omit a member to leave it absent");
        }
        if (v instanceof Raw r) {
            return r.json();
        }
        if (v instanceof String s) {
            return quote(s);
        }
        if (v instanceof Number || v instanceof Boolean) {
            return v.toString();
        }
        throw new IllegalArgumentException("unsupported JSON value " + v.getClass());
    }

    public static String quote(String s) {
        StringBuilder sb = new StringBuilder(s.length() + 2).append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        return sb.append('"').toString();
    }
}
