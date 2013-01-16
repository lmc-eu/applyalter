package ch.ips.g2.applyalter;

import com.thoughtworks.xstream.annotations.XStreamAlias;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * PostgreSQL instance with optional hostname and port
 *
 * @author mcaslavsky
 */
@XStreamAlias("oracle-instance")
public class OracleInstance extends DbInstance {
    protected static final String DB_DRIVER = "oracle.jdbc.driver.OracleDriver";

    public static final String ENGINE = "Oracle";

    protected static void initDriver() {
        try {
            Class.forName(DB_DRIVER);
        } catch (ClassNotFoundException e) {
            throw new ApplyAlterException("Can not initialize db driver " + DB_DRIVER, e);
        }
    }

    public OracleInstance() {
        initDriver();
    }

    public OracleInstance(String id, String type, String host, Integer port, String db, String user, String pass) {
        super(id, type, host, port, db, user, pass);
    }

    @Override
    public Connection getConnection(RunContext ctx)
            throws ApplyAlterException {
        initDriver();
        return super.getConnection(ctx);
    }

    /**
     * Get url for connecting <code>jdbc:postgresql://...</code>
     *
     * @return url for connectiong
     */
    @Override
    public String getUrl() {
        StringBuilder b = new StringBuilder("jdbc:oracle:thin:@");
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
     */
    @Override
    public boolean isSavepointNeededForIgnoredFailure() {
        return true;
    }

    @Override
    public void setSchema(String schema)
            throws ApplyAlterException {
        try {
            DbUtils.executeUpdate(con, "ALTER SESSION SET CURRENT_SCHEMA = " + schema);
        } catch (SQLException e) {
            throw new ApplyAlterException("Can not set schema " + schema, e);
        }
    }

}