package ch.ips.g2.applyalter;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamImplicit;
import com.thoughtworks.xstream.io.xml.PrettyPrintWriter;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.time.DateFormatUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import static ch.ips.g2.applyalter.ReportLevel.*;

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

    //----------------------------------------------------------------------------------------------------------------
    //----------------------------------------------------------------------------------------------------------------

    /**
     * Query the APPLYALTER_PKG table.
     */
    public List<RecordsForDb> executeQuery(@Nullable String hash) {
        List<RecordsForDb> dblist = new ArrayList<RecordsForDb>();
        for (DbInstance d : db.getEntries()) {
            Connection c = d.getConnection(runContext);
            ResultSet r = null;
            PreparedStatement s = null;
            List<Record> records = new ArrayList<Record>();
            try {
                String sql = "SELECT dbid,hash,username,at FROM " + d.getPkgLogTable() + " WHERE dbid=?";
                if (hash != null) {
                    sql += " and hash=?";
                }
                sql += " ORDER BY at DESC";
                s = c.prepareStatement(sql);
                s.setString(1, d.getId());
                if (hash != null) {
                    s.setString(2, hash.toUpperCase());
                }
                r = s.executeQuery();
                while (r.next()) {
                    records.add(new Record(
                            r.getString(2),
                            r.getString(3),
                            r.getTimestamp(4)
                    ));
                }
            } catch (SQLException e) {
                runContext.report(ReportLevel.ERROR, "failed to insert applyalter_pkg record: %s", e.getMessage());
            } finally {
                DbUtils.close(s, r);
            }
            dblist.add(new RecordsForDb(d.getId(), records));
        }
        return dblist;
    }

    /**
     * Execute query and write result to file.
     *
     * @param outputFile path to output file; "-" is handled
     * @param hash       optional search criterion: SHA1 hash value (it is converted to uppercase inside)
     */
    public void queryAndWrite(@Nonnull String outputFile, @Nullable String hash) {
        final boolean stdout = "-".equals(outputFile);

        runContext.report(MAIN, "querying pkg log table");
        final List<PkgLogTableHandler.RecordsForDb> result = executeQuery(hash);
        Writer wr = null;
        try {
            final OutputStream os = stdout ? System.out : new FileOutputStream(outputFile);
            wr = new OutputStreamWriter(os, StandardCharsets.UTF_8);
            XStream xstream = new XStream();
            xstream.processAnnotations(QueryResult.class);
            xstream.marshal(new QueryResult(result), new PrettyPrintWriter(wr));
            wr.flush();
        } catch (IOException e) {
            runContext.report(ERROR, e, "cannot write to file %s", outputFile);
        } finally {
            if (!stdout) {
                IOUtils.closeQuietly(wr);
            }
        }
    }

    @XStreamAlias("query-pkg")
    public static class QueryResult {
        @XStreamImplicit
        protected final List<RecordsForDb> dbs;

        public QueryResult(List<RecordsForDb> dbs) {
            this.dbs = dbs;
        }
    }

    @XStreamAlias("db")
    public static class RecordsForDb {
        public final String id;
        @XStreamImplicit
        protected final List<Record> records;

        public RecordsForDb(String dbid, List<Record> records) {
            this.id = dbid;
            this.records = records;
        }
    }

    @XStreamAlias("record")
    public static class Record {
        public final String hash, username, at;

        public Record(String hash, String username, Timestamp at) {
            this.hash = hash;
            this.username = username;
            this.at = DateFormatUtils.ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT.format(at);
        }
    }


}
