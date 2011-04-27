package ch.ips.g2.applyalter;

import org.testng.Assert;
import org.testng.annotations.Test;

import static ch.ips.g2.applyalter.AlterLoader.bytes2hex;

/**
 */
@Test
public class BytesToHexTest
{

  public void testEmpty()
  {
    Assert.assertEquals( bytes2hex( new byte[0] ), "" );
  }

  public void testSingleByte()
  {
    for ( int i = 0; i < 256; i++ )
      Assert.assertEquals( bytes2hex( new byte[]{(byte) i} ), String.format( "%02X", i ) );
  }

  public void testBytes()
  {
    byte[] b1 = new byte[]{
        15, 0x12, 0x34, 0x56, 0x78, (byte) 0x9a, (byte) 0xbc, 0xd, (byte) 0xef
    };
    Assert.assertEquals( bytes2hex( b1 ), "0F123456789ABC0DEF" );
  }


}
