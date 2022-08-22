package ch.ips.g2.applyalter;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ReplaceInputStreamTest {

    //    @Test(dataProvider = "provider")
    @ParameterizedTest
    @MethodSource("provider")
    public void testReplace(String input, Map<String, String> params, String expOutput) throws IOException {
        assertEquals(expOutput, readAsInputStream(input, params));
    }

    @Test
    public void testParamDoesNotExists() {
        Assertions.assertThrows(IllegalStateException.class, () ->
                readAsInputStream("Hello {{param}} world {{missingParam}}", Map.of("param", "first"))
        );
    }

    private String readAsInputStream(final String input, final Map<String, String> params) throws IOException {
        final Map<String, byte[]> paramMap = params.entrySet().stream().collect(Collectors.toMap(
                Map.Entry::getKey,
                es -> es.getValue().getBytes()
        ));
        final String charsetName = "UTF-8";
        final ByteArrayInputStream is = new ByteArrayInputStream(input.getBytes(charsetName));
        final ReplacingInputStream ris = new ReplacingInputStream(is, paramMap);
        return IOUtils.toString(ris, charsetName);
    }

    private static Stream<Arguments> provider() {
        return Stream.of(
                Arguments.of(
                        "First test",
                        Map.of(),
                        "First test"
                ),
                Arguments.of(
                        "First test",
                        Map.of("db", "test"),
                        "First test"
                ),
                Arguments.of(
                        "First {{}} test",
                        Map.of("db", "test"),
                        "First {{}} test"
                ),

                Arguments.of(
                        "First {{db}} test",
                        Map.of("db", "brand0"),
                        "First brand0 test"
                ),
                Arguments.of(
                        "First {{db}} test\nnext{{db}} line",
                        Map.of("db", "brand0"),
                        "First brand0 test\nnextbrand0 line"
                ),
                Arguments.of(
                        "Hello {{a}} world {{b}} next {{a}} word",
                        Map.of("a", "first", "b", "second"),
                        "Hello first world second next first word"
                ),
                Arguments.of(
                        "Second {{a b}} \nworld {{b\n}} next {{a}} word",
                        Map.of("a", "first"),
                        "Second {{a b}} \nworld {{b\n}} next first word"
                ),
                Arguments.of(
                        "Hello \n{{a-b_c}} world\nand {{def}} next \n xxx {{a-b_c}} word",
                        Map.of("a-b_c", "first", "def", "second"),
                        "Hello \nfirst world\nand second next \n xxx first word"
                ),
                Arguments.of(
                        "Hello {{a}} world {{b}}",
                        Map.of("a", "{{b}}", "b", "{{a}}"),
                        "Hello {{b}} world {{a}}"
                )
        );
    }

}