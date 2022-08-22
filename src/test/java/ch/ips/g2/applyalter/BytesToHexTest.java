package ch.ips.g2.applyalter;

import org.junit.jupiter.api.Test;

import static ch.ips.g2.applyalter.AlterLoader.bytes2hex;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 *
 */
public class BytesToHexTest {

    @Test
    public void testEmpty() {
        assertEquals("", bytes2hex(new byte[0]));
    }

    @Test
    public void testSingleByte() {
        for (int i = 0; i < 256; i++) {
            assertEquals(String.format("%02X", i), bytes2hex(new byte[]{(byte) i}));
        }
    }

    @Test
    public void testBytes() {
        byte[] b1 = new byte[]{
                15, 0x12, 0x34, 0x56, 0x78, (byte) 0x9a, (byte) 0xbc, 0xd, (byte) 0xef
        };
        assertEquals("0F123456789ABC0DEF", bytes2hex(b1));
    }
}
