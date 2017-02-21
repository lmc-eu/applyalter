package ch.ips.g2.applyalter;

import com.thoughtworks.xstream.annotations.XStreamAlias;

import java.sql.Connection;

/**
 * PostgreSQL instance with optional hostname and port
 *
 * @author mcaslavsky
 */
@XStreamAlias("mysql-instance")
public class MysqlInstance extends DbInstance {
    protected static final String DB_DRIVER = "com.mysql.jdbc.Driver";

    public static final String ENGINE = "MySQL";

    protected static void initDriver() {
        try {
            Class.forName(DB_DRIVER);
        } catch (ClassNotFoundException e) {
            throw new ApplyAlterException("Can not initialize db driver " + DB_DRIVER, e);
        }
    }

    public MysqlInstance() {
        initDriver();
    }

    public MysqlInstance(String id, String type, String host, Integer port, String db, String user, String pass) {
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
        return buildCommonUrl("jdbc:mysql:").toString();
    }

    @Override
    public String getEngine() {
        return ENGINE;
    }

    @Override
    public void setSchema(String schema) {
        //do nothing; mysql does not support schema
    }

    @Override
    public String getLogTable() {
        return "applyalter_log";
    }

    @Override
    public String getPkgLogTable() {
        return "applyalter_pkg";
    }
}