package ch.ips.g2.applyalter;

import com.google.common.collect.ImmutableMap;
import org.apache.commons.io.IOUtils;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Map;
import java.util.stream.Collectors;

public class ReplaceInputStreamTest {

    @Test(dataProvider = "provider")
    public void testReplace(String input, Map<String, String> params, String expOutput) throws IOException {
        Assert.assertEquals(readAsInputStream(input, params), expOutput);
    }

    @Test(expectedExceptions = IllegalStateException.class)
    public void testParamDoesNotExists() throws IOException {
        readAsInputStream("Hello {{param}} world {{missingParam}}", ImmutableMap.of("param", "first"));
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

    @DataProvider
    private Object[][] provider() {
        return new Object[][]{
                new Object[]{
                        "First test",
                        ImmutableMap.of(),
                        "First test"
                },
                new Object[]{
                        "First test",
                        ImmutableMap.of("db", "test"),
                        "First test"
                },
                new Object[]{
                        "First {{}} test",
                        ImmutableMap.of("db", "test"),
                        "First {{}} test"
                },

                new Object[]{
                        "First {{db}} test",
                        ImmutableMap.of("db", "brand0"),
                        "First brand0 test"
                },
                new Object[]{
                        "First {{db}} test\nnext{{db}} line",
                        ImmutableMap.of("db", "brand0"),
                        "First brand0 test\nnextbrand0 line"
                },
                new Object[]{
                        "Hello {{a}} world {{b}} next {{a}} word",
                        ImmutableMap.of("a", "first", "b", "second"),
                        "Hello first world second next first word"
                },
                new Object[]{
                        "Second {{a b}} \nworld {{b\n}} next {{a}} word",
                        ImmutableMap.of("a", "first"),
                        "Second {{a b}} \nworld {{b\n}} next first word"
                },
                new Object[]{
                        "Hello \n{{a-b_c}} world\nand {{def}} next \n xxx {{a-b_c}} word",
                        ImmutableMap.of("a-b_c", "first", "def", "second"),
                        "Hello \nfirst world\nand second next \n xxx first word"
                },
                new Object[]{
                        "Hello {{a}} world {{b}}",
                        ImmutableMap.of("a", "{{b}}", "b", "{{a}}"),
                        "Hello {{b}} world {{a}}"
                }

        };
    }

}