package ch.ips.g2.applyalter;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/**
 * PostgreSQL instance with optional hostname and port
 *
 * @author mcaslavsky
 */
@XStreamAlias("pginstance")
public class PgInstance extends DbInstance {
    protected static final String DB_DRIVER = "org.postgresql.Driver";

    public static final String ENGINE = "Postgresql";

    public String role;

    protected static void initDriver() {
        try {
            Class.forName(DB_DRIVER);
        } catch (ClassNotFoundException e) {
            throw new ApplyAlterException("Can not initialize db driver " + DB_DRIVER, e);
        }
    }

    public PgInstance() {
        initDriver();
    }

    public PgInstance(String id, String type, String host, Integer port, String db, String user, String pass) {
        super(id, type, host, port, db, user, pass);
    }


    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    /**
     * Get url for connecting <code>jdbc:postgresql://...</code>
     *
     * @return url for connectiong
     */
    @Override
    public String getUrl() {
        StringBuilder b = new StringBuilder("jdbc:postgresql:");
        if (host != null) {
            b.append("//").append(host);
            if (port != null)
                b.append(":").append(port);
            b.append("/");
        }
        b.append(db);
        return b.toString();
    }

    @Override
    public String getEngine() {
        return ENGINE;
    }

    /**
     * Special flag for "canfail" and "ignore-sql{code|state}" support on postgresql.
     * On failure, postgresql marks transaction for rollback. When we need to ignore that error,
     * statement must be done inside savepoint.
     *
     * @return true postgresql needs the savepoint
     */
    @Override
    public boolean isSavepointNeededForIgnoredFailure() {
        return true;
    }

    @Override
    public void setSchema(String schema)
            throws ApplyAlterException {
        try {
            DbUtils.executeUpdate(con, "set search_path to " + schema + ",public");
        } catch (SQLException e) {
            throw new ApplyAlterException("Can not set schema " + schema, e);
        }
    }

    @Override
    protected Connection connect(String url, RunContext ctx)
            throws SQLException {
        if (getPass() == null || getPass().trim().length() < 1) {
            //read password from the pgpass file; see http://www.postgresql.org/docs/current/static/libpq-pgpass.html
            loadPgpass(ctx);
        }

        final Connection con = super.connect(url, ctx);
        if (getRole() != null) {
            try {
                DbUtils.executeUpdate(con, "set role to " + getRole());
            } catch (SQLException e) {
                throw new ApplyAlterException("Can not set role to: " + getRole(), e);
            }
        }
        return con;
    }


    @Override
    public String getLogTable() {
        return "public.applyalter_log";
    }


    /**
     * Load and parse <a href="http://www.postgresql.org/docs/current/static/libpq-pgpass.html">pgpass file</a>, try to find
     * matching line and if found, set password via {@link #setPass(String)}
     * @param runContext context (used by logging)
     */
    private void loadPgpass(RunContext runContext) {
        File pgpassFile = findPgpassFile(runContext);
        if (pgpassFile == null)
            return;
        runContext.report(ReportLevel.MAIN, "loading password file: %s", pgpassFile);
        //load whole file at once
        final List<String> lines;
        try {
            lines = IOUtils.readLines(new FileInputStream(pgpassFile), "UTF-8");
        } catch (IOException e) {
            runContext.report(ReportLevel.FATAL, e, "error loading password file: %s", pgpassFile);
            return;
        }
        //and parse all lines until match is found
        for (int lineNum = 0, linesSize = lines.size(); lineNum < linesSize; lineNum++) {
            String line = lines.get(lineNum).trim();
            if (line.length() < 1 || line.startsWith("#")) {
                //empty line or comment
                continue;
            }
            //hostname:port:database:username:password
            final String[] split = line.split(":");
            if (split.length < 5) {
                runContext.report(ReportLevel.ERROR, "invalid password file: %s (line %d)", pgpassFile, lineNum);
                continue;
            }
            if (!fieldMatch(split[0], getHost()))
                continue;
            String portStr = getPort() == null ? null : getPort().toString();
            if (!fieldMatch(split[1], portStr))
                continue;
            if (!fieldMatch(split[2], getDb()))
                continue;
            if (!fieldMatch(split[3], getUser()))
                continue;
            //match found!
            setPass(split[4]);
            runContext.report(ReportLevel.STATEMENT, "match found in password file for user %s", getUser());
            return;
        }
    }

    /**
     * Compare fieldValue from pgpass file with my value from this configuration file.
     *
     * @param fieldValue filed value, "*" means everything; never null
     * @param myValue    my configuration value; might be null, in which case it always matches
     * @return true when they are equal or either of them is special
     */
    private boolean fieldMatch(String fieldValue, String myValue) {
        return "*".equals(fieldValue) || myValue == null || fieldValue.equals(myValue);
    }

    public static final String ENV_PGPASSFILE = "PGPASSFILE";

    private File findPgpassFile(RunContext runContext) {
        final String envVar = System.getenv(ENV_PGPASSFILE);
        if (envVar != null) {
            File f = new File(envVar);
            if (f.exists() && f.isFile() && f.canRead()) {
                return f;
            }
            runContext.report(ReportLevel.ERROR, "invalid content of environment variable: %s=%s", ENV_PGPASSFILE, envVar);
        }
        File f = new File(System.getProperty("user.home"), ".pgpass");
        if (f.exists() && f.isFile() && f.canRead()) {
            return f;
        }
        runContext.report(ReportLevel.DETAIL, "missing password file %s", f);
        return null;
    }


}
