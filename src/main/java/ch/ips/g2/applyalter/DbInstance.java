package ch.ips.g2.applyalter;

import com.thoughtworks.xstream.annotations.XStreamOmitField;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Database instance configuration. This class is not thread safe.
 * Database instance can be "fake": just omit the <code>db</code> tag.
 *
 * @author Martin Caslavsky &lt;martin.caslavsky@ips-ag.cz&gt;
 * @version $Id$
 */
public abstract class DbInstance {

    public String id;
    public String type;
    public String host;
    public Integer port;
    public String db;
    public String user;
    public String pass;
    /**
     * Extra properties.
     */
    protected Properties properties;
    @XStreamOmitField
    protected Connection con;
    @XStreamOmitField
    protected boolean used;


    public DbInstance() {
        super();
    }

    public DbInstance(String id, String type, String host, Integer port, String db, String user, String pass) {
        this();
        this.id = id;
        this.type = type;
        this.host = host;
        this.port = port;
        this.db = db;
        this.user = user;
        this.pass = pass;
    }

    /**
     * Get url for connecting <code>jdbc:db2://...</code>
     *
     * @return url for connectiong
     */
    public abstract String getUrl();

    /**
     * Build semi-standard URL for most JDBC connectors. Used by postgres, mysql, oracle.
     *
     * @param base base prefix, inclusing all neccesary colon but not slashes
     * @return full URL with base, host (if present), port (if present) and db name
     */
    protected StringBuilder buildCommonUrl(String base) {
        StringBuilder b = new StringBuilder(base);
        if (host != null) {
            b.append("//").append(host);
            if (port != null)
                b.append(":").append(port);
            b.append("/");
        }
        b.append(db);
        return b;
    }

    /**
     * Engine identification: "DB2", "Postgresql", etc.
     */
    public abstract String getEngine();

    /**
     * Special flag for "canfail" and "ignore-sql{code|state}" support on postgresql.
     * On failure, postgresql marks transaction for rollback. When we need to ignore that error,
     * statement must be done inside savepoint.
     *
     * @return true=database does implicit rollback, savepoints needed; false = no special treatment needed
     */
    public boolean isSavepointNeededForIgnoredFailure() {
        //DB2 does not need this
        return false;
    }

    /**
     * Change database schema on this databse instance
     *
     * @param schema schema name to set
     * @throws ApplyAlterException if schema can not be set
     */
    public void setSchema(String schema) throws ApplyAlterException {
        //important: DB2 requires uppercase schema!
        schema = schema.toUpperCase();

        try {
            con.setCatalog(schema);

            DbUtils.executeUpdate(con, "set schema ?", schema);

        } catch (SQLException e) {
            throw new ApplyAlterException("Can not set schema " + schema, e);
        }
    }

    /**
     * Set transaction isolation level.
     * Connection must be open.
     *
     * @param isolation isolation level; null = do not change
     * @throws ApplyAlterException error setting
     */
    public void setIsolation(IsolationLevel isolation)
            throws ApplyAlterException {
        if (isolation == null)
            return;

        try {
            con.setTransactionIsolation(isolation.getJdbcValue());
        } catch (SQLException e) {
            throw new ApplyAlterException("Cannot set isolation level to %s (%d)", e,
                    isolation, isolation.getJdbcValue());
        }
    }

    /**
     * Get a current connection to this database instance with auto commit off.
     * Default auto commit can be changed by {@link DbConfig#setDbAutocommit(boolean)}
     *
     * @param ctx
     * @return connection to this database instance
     * @throws ApplyAlterException if connection could not be acquired
     */
    public Connection getConnection(RunContext ctx) throws ApplyAlterException {
        if (con == null) {
            String url = getUrl();
            try {
                con = connect(url, ctx);
                con.setAutoCommit(DbConfig.AUTOCOMMIT_OFF);
            } catch (SQLException e) {
                throw new ApplyAlterException("Can not acquire db connection for " + url, e);
            }
        }
        return con;
    }

    public void setAutoCommit(boolean autocommit) {
        if (con != null) {
            try {
                con.setAutoCommit(autocommit);
            } catch (SQLException e) {
                throw new ApplyAlterException("Failed to set autocommit to " + autocommit + " for database url:" + getUrl(), e);
            }
        }
    }

    public boolean getAutoCommit() {
        if (con != null) {
            try {
                return con.getAutoCommit();
            } catch (SQLException e) {
                throw new RuntimeException("Failed to get autocommit for database url:" + getUrl(), e);
            }
        }
        throw new RuntimeException("No connection established for url: " + getUrl());
    }

    /**
     * The real implementation of {@link #getConnection(RunContext)}. Default implementation just calls
     * {@link DriverManager#getConnection(String, String, String)}, subclass can obtain the connection
     * by some other way.
     *
     * @param url JDBC url, made by {@link #getUrl()}
     * @param ctx
     * @return new connection
     * @throws SQLException error getting connection
     */
    protected Connection connect(String url, RunContext ctx)
            throws SQLException {
        return DriverManager.getConnection(url, makeConnectionProperties());
    }

    /**
     * Create full connection properties for {@link DriverManager#getConnection(String, Properties)}.
     *
     * @return new instance of properties (called can modify)
     */
    protected Properties makeConnectionProperties() {
        Properties info = new Properties();
        if (this.properties != null) {
            info.putAll(this.properties);
        }
        //following two conditions are actually defined by JDBC standard, but there are no constans for the keys :-(
        if (user != null && user.length() > 0) {
            info.put("user", user);
        }
        if (pass != null) { //note: password can actually be empty
            info.put("password", pass);
        }
        return info;
    }

    /**
     * Get a current connection to this database instance and mark it as used
     *
     * @return connection to this database instance
     * @throws ApplyAlterException if connection could not be acquired
     * @see #getConnection(RunContext)
     */
    public Connection markConnectionUsed(RunContext ctx) throws ApplyAlterException {
        Connection c = getConnection(ctx);
        used = true;
        return c;
    }

    /**
     * Close connection
     */
    public void closeConnection() {
        if (con != null) {
            try {
                con.close();
            } catch (SQLException e) {
                //ignore
            }
            con = null;
        }
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    /**
     * Get database type; either explicit one, or just {@link #getId()}.
     *
     * @return database type, should not be null
     */
    public String getType() {
        if (type == null) {
            return getId();
        }
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public boolean isUsed() {
        return used;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public String getDb() {
        return db;
    }

    public void setDb(String db) {
        this.db = db;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getPass() {
        return pass;
    }

    public void setPass(String pass) {
        this.pass = pass;
    }


    /**
     * Is this database connect valid, non-fake?
     *
     * @return true=real, false=fake
     */
    public boolean isReal() {
        return getDb() != null;
    }

    public String getLogTable() {
        return "wasg2.applyalter_log";
    }

    public String getPkgLogTable() {
        return "wasg2.applyalter_pkg";
    }

    //--------------------------------------------------------------------------------------------------

    protected static final String[] MDTYPES_TABLE = {"TABLE", "ALIAS"};
    protected static final String[] MDTYPES_VIEW = {"VIEW"};

    /**
     * Process the Check for this database instance.
     * Default implementation checks by using JDBC metadata, but some checks are not supported.
     *
     * @throws SQLException                  database error
     * @throws UnsupportedOperationException unsupported check type
     */
    public boolean check(RunContext runContext, Connection con, Check chk, String schema)
            throws SQLException, UnsupportedOperationException {
        final DatabaseMetaData md = con.getMetaData();
        final String name = chk.getName();
        final String table = chk.getTable();
        ResultSet rs = null;
        try {

            switch (chk.getType()) {
                case table:
                    rs = md.getTables(null, schema, name, MDTYPES_TABLE);
                    break;
                case view:
                    rs = md.getTables(null, schema, name, MDTYPES_VIEW);
                    break;
                case column:
                    rs = md.getColumns(null, schema, table, name);
                    break;
                case index:
                    rs = md.getIndexInfo(null, schema, table, false, true);
                    //there is no way to pass in index name --> we have to iterate resultset and find it
                    while (rs.next()) {
                        final String index_name = rs.getString("INDEX_NAME");
                        if (name.equalsIgnoreCase(index_name)) {
                            return true ^ chk.isInverted();
                        }
                    }
                    return false ^ chk.isInverted();
                default:
                    throw new UnsupportedOperationException(
                            getClass().getSimpleName() + " does not support check type " + chk.getType()
                    );
            }

            boolean rawResult = rs.next();
            //XOR with the "isInverted" flag
            return rawResult ^ chk.isInverted();

        } finally {
            DbUtils.close(null, rs);
        }
    }

    //------------------------------------------------------------------------------------------------------------------

    /**
     * Regular expression matching LMC environment
     */
    protected static final Pattern LMC_HOSTNAME_PATTERN = Pattern.compile("(\\w+)\\.(\\w+).(.*)");

    /**
     * Guess environment: LMC-specific feature.
     * By default, second component from {@link #host} is considered environment (db2nas.devel.lmc.cz = devel).
     *
     * @param runContext run contet (used for logging)
     * @return environment, null if guess fails
     * <br /> might be non-normalized (dev1, for example).
     */
    public String guessEnvironment(RunContext runContext) {
        if (getHost() == null)
            return null;
        final Matcher matcher = LMC_HOSTNAME_PATTERN.matcher(getHost());
        if (matcher.matches()) {
            String env = matcher.group(2);
            runContext.report(ReportLevel.DETAIL, "environment %s guessed from: %s", env, getHost());
            return env;
        } else {
            runContext.report(ReportLevel.DETAIL, "environment cannot be guessed from: %s", getHost());
            return null;
        }
    }

    //------------------------------------------------------------------------------------------------------------------
    // dialect-specific methods

    /**
     * Decide the fully classified table name for temporary table.
     *
     * @param tableBaseName Base table name (unqualified)
     * @return Fully classified table name
     */
    public String makeTemporaryTableName(String tableBaseName) {
        return tableBaseName;
    }

    /**
     * Return SQL creating temporary table with given name, created from given query.
     *
     * @param tableName Qualified table name
     * @param query     Select query which defines table structure and data
     * @return SQL
     */
    public String makeCreateTemporaryTableAsSql(String tableName, String query) {
        throw new UnsupportedOperationException("Temporary tables not implemented for current database type");
    }
}
