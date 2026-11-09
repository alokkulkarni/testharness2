package app.todo.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/** sdd.md section 5: whitespace is exactly the 25 code points of the Unicode White_Space property. */
class WhitespaceTest {

    static Stream<Integer> whitespaceCodePoints() {
        return Stream.of(
                        IntStream.rangeClosed(0x0009, 0x000D).boxed(),
                        Stream.of(0x0020, 0x0085, 0x00A0, 0x1680),
                        IntStream.rangeClosed(0x2000, 0x200A).boxed(),
                        Stream.of(0x2028, 0x2029, 0x202F, 0x205F, 0x3000))
                .flatMap(s -> s);
    }

    static Stream<Integer> notWhitespaceCodePoints() {
        return Stream.of(
                0x001C, 0x001D, 0x001E, 0x001F, // Java's trim()/strip() would remove these
                0x200B, 0x200C, 0x200D, 0x2060, 0xFEFF, // zero-width format characters are kept
                (int) 'a', (int) '0', 0x1F600);
    }

    @Test
    @DisplayName("REQ-TODO-002 AC-1 the whitespace set has exactly 25 code points")
    void REQ_TODO_002_AC1_whitespace_set_has_25_code_points() {
        assertThat(whitespaceCodePoints().count()).isEqualTo(25);
    }

    @ParameterizedTest(name = "U+{0}")
    @MethodSource("whitespaceCodePoints")
    @DisplayName("REQ-TODO-002 AC-1 every White_Space code point is whitespace and is trimmed")
    void REQ_TODO_002_AC1_each_white_space_code_point_is_trimmed(int codePoint) {
        String ws = new String(Character.toChars(codePoint));
        assertThat(Whitespace.isWhitespace(codePoint)).isTrue();
        assertThat(Whitespace.trim(ws + "ab" + ws)).isEqualTo("ab");
        assertThat(Whitespace.trim(ws + ws)).isEmpty();
        assertThat(Whitespace.isBlank(ws)).isTrue();
    }

    @ParameterizedTest(name = "U+{0}")
    @MethodSource("notWhitespaceCodePoints")
    @DisplayName("REQ-TODO-002 AC-1 other code points (U+001C..U+001F, zero-width, U+FEFF) are kept")
    void REQ_TODO_002_AC1_other_code_points_are_not_trimmed(int codePoint) {
        String c = new String(Character.toChars(codePoint));
        assertThat(Whitespace.isWhitespace(codePoint)).isFalse();
        assertThat(Whitespace.trim(c + "ab" + c)).isEqualTo(c + "ab" + c);
        assertThat(Whitespace.isBlank(c)).isFalse();
    }

    @Test
    @DisplayName("REQ-TODO-002 AC-2 a value of only non-breaking and other Unicode spaces is blank")
    void REQ_TODO_002_AC2_value_of_only_unicode_spaces_is_blank() {
        assertThat(Whitespace.isBlank("")).isTrue();
        assertThat(Whitespace.isBlank(" ")).isTrue();
        assertThat(Whitespace.isBlank("  ")).isTrue();
        assertThat(Whitespace.isBlank("　")).isTrue();
        assertThat(Whitespace.isBlank(" \t\n\u0085")).isTrue();
        assertThat(Whitespace.isBlank(" a ")).isFalse();
    }

    @Test
    @DisplayName("REQ-TODO-002 AC-1 trim removes only leading and trailing whitespace")
    void REQ_TODO_002_AC1_trim_keeps_interior_whitespace() {
        assertThat(Whitespace.trim(" 　 a   b　 ")).isEqualTo("a   b");
        assertThat(Whitespace.trim("plain")).isEqualTo("plain");
    }

    @Test
    @DisplayName("REQ-LIST-002 AC-7 search text of only whitespace trims to empty")
    void REQ_LIST_002_AC7_search_text_of_only_whitespace_trims_to_empty() {
        assertThat(Whitespace.trim("   　  ")).isEmpty();
    }
}
