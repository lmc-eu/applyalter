package ch.ips.g2.applyalter;

import java.io.ByteArrayInputStream;
import org.testng.annotations.Test;

import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

@Test
public class PgInstanceTest {

  // hostname:port:database:username:password
  public void loadPgPassResolvePassForName() throws Exception {
    PgInstance pg = new PgInstance("id", "type", "localhost", 5432, "main", "dbadmin", null);
    String pgpass = "*:*:*:*:secret";
    RunContext ctx = mock(RunContext.class);
    pg.loadPgpass(ctx, new ByteArrayInputStream(pgpass.getBytes("UTF-8")));

    assertEquals(pg.getUser(), "dbadmin");
    assertEquals(pg.getPass(), "secret");
  }

  public void loadPgPassResolvePassForMatchingName() throws Exception {
    PgInstance pg = new PgInstance("id", "type", "localhost", 5432, "main", "dbadmin", null);
    String pgpass = "*:*:*:dbuser:weak\n" +
            "*:*:*:dbadmin:secret";
    RunContext ctx = mock(RunContext.class);
    pg.loadPgpass(ctx, new ByteArrayInputStream(pgpass.getBytes("UTF-8")));

    assertEquals(pg.getUser(), "dbadmin");
    assertEquals(pg.getPass(), "secret");
  }

  public void loadPgPassResolveUserPass() throws Exception {
    PgInstance pg = new PgInstance("id", "type", "localhost", 5432, "main", null, null);
    String pgpass = "*:*:*:dbadmin:secret";
    RunContext ctx = mock(RunContext.class);
    pg.loadPgpass(ctx, new ByteArrayInputStream(pgpass.getBytes("UTF-8")));

    assertEquals(pg.getUser(), "dbadmin");
    assertEquals(pg.getPass(), "secret");
  }
}
