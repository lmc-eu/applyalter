package ch.ips.g2.applyalter;

import java.sql.*;

/**
 * Some small database utilities.
 *
 * @author Kamil Podlesak &lt;kamil.podlesak@ips-ag.cz&gt;
 * @version $Id$
 */
public class DbUtils {

    /**
     * Close statement, do nothing if it's not open.
     * No exception is thrown, ever; this method is thus safe to use inside finally clause.
     *
     * @param stmt statement to close, null is handled (nothing is done in such case)
     */
    public static void close(Statement stmt) {
        if (stmt == null)
            return;
        try {
            stmt.close();
        } catch (SQLException e) {
            //ignore
        }
    }

    /**
     * Close result set and statement (any of them or both can be missing).
     * No exception is thrown, ever; this method is thus safe to use inside finally clause.
     *
     * @param stmt statement to close, null is handled (nothing is done in such case)
     * @param rs   result set to close, null is handled (nothing is done in such case)
     */
    public static void close(Statement stmt, ResultSet rs) {
        if (rs != null) {
            try {
                rs.close();
            } catch (SQLException e) {
                //ignore
            }
        }
        close(stmt);
    }

    /**
     * Execute simple update query.
     * Remember that this method does not support NULL values, because there is no way to pass type of such NULL
     * value.
     *
     * @param con        connection
     * @param sql        query to execute, might contain placeholders
     * @param parameters parameters for placeholders (not: NULL values are not supported by DB2 JDBC!)
     * @return number of changed rows
     * @throws SQLException error executing query
     * @see java.sql.PreparedStatement#setObject(int, Object)
     */
    public static int executeUpdate(Connection con, String sql, Object... parameters)
            throws SQLException {
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement(sql);

            for (int i = 0; i < parameters.length; i++) {
                ps.setObject(i + 1, parameters[i]);
            }

            return ps.executeUpdate();
        } finally {
            DbUtils.close(ps);
        }
    }

}
