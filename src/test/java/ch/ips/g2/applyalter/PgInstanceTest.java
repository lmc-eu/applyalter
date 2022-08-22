package ch.ips.g2.applyalter;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

public class PgInstanceTest {

    // hostname:port:database:username:password
    @Test
    public void loadPgPassResolvePassForName() throws Exception {
        PgInstance pg = new PgInstance("id", "type", "localhost", 5432, "main", "dbadmin", null);
        String pgpass = "*:*:*:*:secret";
        RunContext ctx = mock(RunContext.class);
        pg.loadPgpass(ctx, new ByteArrayInputStream(pgpass.getBytes(StandardCharsets.UTF_8)));

        assertEquals("dbadmin", pg.getUser());
        assertEquals("secret", pg.getPass());
    }

    @Test
    public void loadPgPassResolvePassForMatchingName() throws Exception {
        PgInstance pg = new PgInstance("id", "type", "localhost", 5432, "main", "dbadmin", null);
        String pgpass = "*:*:*:dbuser:weak\n" +
                "*:*:*:dbadmin:secret";
        RunContext ctx = mock(RunContext.class);
        pg.loadPgpass(ctx, new ByteArrayInputStream(pgpass.getBytes(StandardCharsets.UTF_8)));

        assertEquals("dbadmin", pg.getUser());
        assertEquals("secret", pg.getPass());
    }

    @Test
    public void loadPgPassResolveUserPass() throws Exception {
        PgInstance pg = new PgInstance("id", "type", "localhost", 5432, "main", null, null);
        String pgpass = "*:*:*:dbadmin:secret";
        RunContext ctx = mock(RunContext.class);
        pg.loadPgpass(ctx, new ByteArrayInputStream(pgpass.getBytes(StandardCharsets.UTF_8)));

        assertEquals("dbadmin", pg.getUser());
        assertEquals("secret", pg.getPass());
    }
}
