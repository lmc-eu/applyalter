package ch.ips.g2.applyalter;

import com.thoughtworks.xstream.annotations.XStreamAlias;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Standard db2 instance.
 *
 * @author Kamil Podlesak &lt;kamil.podlesak@ips-ag.cz&gt;
 * @version $Id$
 */
@XStreamAlias("dbinstance")
public class Db2Instance extends DbInstance {
    protected static final String DB_DRIVER = "com.ibm.db2.jcc.DB2Driver";

    public static final String ENGINE = "DB2";

    static {
        try {
            Class.forName(DB_DRIVER);
        } catch (ClassNotFoundException e) {
            throw new ApplyAlterException("Can not initialize db driver " + DB_DRIVER, e);
        }
    }

    public Db2Instance() {
    }

    public Db2Instance(String id, String type, String host, Integer port, String db, String user, String pass) {
        super(id, type, host, port, db, user, pass);
    }


    /**
     * Get url for connecting <code>jdbc:db2://...</code>
     *
     * @return url for connectiong
     */
    @Override
    public String getUrl() {
        return String.format("jdbc:db2://%s:%s/%s", host, port, db);
    }

    /**
     * Engine identification: "DB2", "Postgresql", etc.
     */
    @Override
    public String getEngine() {
        return ENGINE;
    }

    //--------------------------------------------------------------------------------------------------

    protected String makeCheckSql(CheckType cht) {
        String sql = String.format("select * from syscat.%s where %sschema=? and %sname=?",
                cht.getTbl(), cht.getAbbr(), cht.getAbbr());
        if (cht.getExtra() != null)
            sql += String.format(" and %s=?", cht.getExtra());
        return sql;
    }

    /**
     * Check if object exists in database, which means an alter was applied already.
     *
     * @param runContext
     * @param c          Connection to database
     * @param a          check object @return true if object exists in database @throws ApplyAlterException
     */
    @Override
    public boolean check(RunContext runContext, Connection c, Check a, String schema)
            throws SQLException {
        PreparedStatement s = null;
        ResultSet rs = null;
        try {
            final String sql = makeCheckSql(a.getType());
            StringBuilder buf = new StringBuilder();
            buf.append("Check: ").append(sql).append(" (");

            s = c.prepareStatement(sql);
            int i = 1;
            schema = schema.toUpperCase();
            s.setString(i++, schema);
            buf.append(schema).append(' ');
            if (a.table != null) {
                String table = a.getTable().toUpperCase();
                buf.append(table).append(' ');
                s.setString(i++, table);
            }
            String name = a.getName().toUpperCase();
            buf.append(name);
            s.setString(i++, name);
            buf.append(")");

            runContext.report(ReportLevel.STATEMENT_STEP, "%s", buf);

            rs = s.executeQuery();
            boolean rawResult = rs.next();

            //XOR with the "isInverted" flag
            return rawResult ^ a.isInverted();

        } finally {
            DbUtils.close(s, rs);
        }
    }

}
