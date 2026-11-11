package app.todo.common;

/**
 * The one definition of whitespace for the API (sdd.md section 5, REQ-TODO-002 AC-1, REQ-LIST-002 AC-7): exactly the
 * 25 code points of the Unicode {@code White_Space} property. Java's {@code String.strip()} and {@code trim()} are not
 * used because each differs from this set; zero-width format characters (U+200B..U+200D, U+2060, U+FEFF) are not
 * whitespace and are kept.
 */
public final class Whitespace {

    private Whitespace() {}

    public static boolean isWhitespace(int codePoint) {
        return switch (codePoint) {
            case 0x0009, 0x000A, 0x000B, 0x000C, 0x000D, 0x0020, 0x0085, 0x00A0, 0x1680,
                    0x2000, 0x2001, 0x2002, 0x2003, 0x2004, 0x2005, 0x2006, 0x2007, 0x2008, 0x2009, 0x200A,
                    0x2028, 0x2029, 0x202F, 0x205F, 0x3000 -> true;
            default -> false;
        };
    }

    /** Removes leading and trailing whitespace code points; interior whitespace is kept. */
    public static String trim(String value) {
        int start = 0;
        int end = value.length();
        while (start < end) {
            int cp = value.codePointAt(start);
            if (!isWhitespace(cp)) {
                break;
            }
            start += Character.charCount(cp);
        }
        while (end > start) {
            int cp = value.codePointBefore(end);
            if (!isWhitespace(cp)) {
                break;
            }
            end -= Character.charCount(cp);
        }
        return value.substring(start, end);
    }

    public static boolean isBlank(String value) {
        return trim(value).isEmpty();
    }
}
