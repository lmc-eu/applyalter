package ch.ips.g2.applyalter;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import static ch.ips.g2.applyalter.ReportLevel.MAIN;

/**
 * All code that works with APPLYALTER_PKG table is here, in this class.
 */
class PkgLogTableHandler {
    protected final RunContext runContext;
    protected final DbConfig db;
    protected final String username;

    public PkgLogTableHandler(RunContext runContext, DbConfig db, String username) {
        this.runContext = runContext;
        this.db = db;
        this.username = username;
    }

    /**
     * Log record to applyalter_pkg. Unlike record to applyalter_log, this one does commit!
     *
     * @param sourceHash hash of source bundle
     */
    protected void savelog_pkg(String sourceHash) {
        if (sourceHash == null || runContext.getRunMode() != RunMode.SHARP) {
            //do not write to database
            return;
        }

        for (DbInstance d : db.getEntries()) {
            savelog_pkg(d, sourceHash);
        }
    }

    /**
     * Log record to applyalter_pkg. Unlike record to applyalter_log, this one does commit!
     *
     * @param d          database instance
     * @param sourceHash hash of source bundle
     */
    private void savelog_pkg(DbInstance d, String sourceHash) {
        String id = d.getId();
        runContext.report(MAIN, "Package record: %s/%s", id, sourceHash);

        Connection c = d.getConnection(runContext);
        PreparedStatement s = null;
        try {
            s = c.prepareStatement("insert into " + d.getPkgLogTable() + " (dbid,hash,username) values (?,?,?)");
            s.setString(1, id);
            s.setString(2, sourceHash);
            s.setString(3, username);
            s.executeUpdate();
            c.commit();
        } catch (SQLException e) {
            runContext.report(ReportLevel.ERROR, "failed to insert applyalter_pkg record: %s", e.getMessage());
        } finally {
            DbUtils.close(s);
        }
    }


}
