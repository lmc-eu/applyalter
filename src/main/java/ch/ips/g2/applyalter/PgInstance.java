package ch.ips.g2.applyalter;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/**
 * PostgreSQL instance with optional hostname and port.
 *
 * @author mcaslavsky
 */
@XStreamAlias("pginstance")
public class PgInstance extends DbInstance {
    protected static final String DB_DRIVER = "org.postgresql.Driver";

    public static final String ENGINE = "Postgresql";

    private static final String SQL_CREATE_TEMPORARY_TABLE = "create temporary table %s AS %s";

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
        return buildCommonUrl("jdbc:postgresql:").toString();
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
            loadPgpass(ctx, findPgpassFile(ctx));
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

    @Override
    public String getPkgLogTable() {
        return "public.applyalter_pkg";
    }

    /**
     * Load and parse <a href="http://www.postgresql.org/docs/current/static/libpq-pgpass.html">pgpass file</a>, try to find
     * matching line and if found, set password via {@link #setPass(String)}
     * @param runContext context (used by logging)
     */
    void loadPgpass(RunContext runContext, InputStream pgpassInputStream) {
        if (pgpassInputStream == null)
            return;
        //load whole file at once
        final List<String> lines;
        try {
            lines = IOUtils.readLines(pgpassInputStream, "UTF-8");
        } catch (IOException e) {
            runContext.report(ReportLevel.FATAL, e, "error loading password file");
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
                runContext.report(ReportLevel.ERROR, "invalid password file: (line %d)", lineNum);
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
            if (getUser() == null && !"*".equals(split[3])) {
              setUser(split[3]);
            }
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

    private InputStream findPgpassFile(RunContext runContext) {
        final String envVar = System.getenv(ENV_PGPASSFILE);
        try {
          if (envVar != null) {
              File f = new File(envVar);
              if (f.exists() && f.isFile() && f.canRead()) {
                  runContext.report(ReportLevel.MAIN, "loading password file: %s", f);
                  return new FileInputStream(f);
              }
              runContext.report(ReportLevel.ERROR, "invalid content of environment variable: %s=%s", ENV_PGPASSFILE, envVar);
          }
          File f = new File(System.getProperty("user.home"), ".pgpass");
          if (f.exists() && f.isFile() && f.canRead()) {
              runContext.report(ReportLevel.MAIN, "loading password file: %s", f);
              return new FileInputStream(f);
          }
          runContext.report(ReportLevel.DETAIL, "missing password file %s", f);
        } catch (IOException ioe) {
          runContext.report(ReportLevel.FATAL, ioe, "cannot read password file.");
        }
        return null;
    }

    @Override
    public String makeCreateTemporaryTableAsSql(String tableName, String query) {
        return String.format(SQL_CREATE_TEMPORARY_TABLE, tableName, query);
    }
}
