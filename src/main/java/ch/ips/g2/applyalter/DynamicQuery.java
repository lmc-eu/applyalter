package ch.ips.g2.applyalter;

import com.thoughtworks.xstream.annotations.XStreamAlias;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Dynamic sql statement: the provided query is executed and its result must contain queries to execute.
 *
 * @author Kamil Podlesak &lt;kamil.podlesak@ips-ag.cz&gt;
 * @version $Id$
 */
@XStreamAlias("dynamic")
public class DynamicQuery extends AbstractStatement {

    public void execute(DbInstance dbConn, RunContext ctx, Map<String, byte[]> datafiles)
            throws ApplyAlterException, SQLException {
        Connection connection = dbConn.getConnection(ctx);
        List<String> queries = generateQueries(ctx, connection, datafiles);

        for (String sql : queries) {
            PreparedStatement st = null;
            try {
                //important: datafile expanding is NOT done here!
                ctx.report(ReportLevel.STATEMENT_STEP, "executing dynamic statement: %s%n", sql);
                st = connection.prepareStatement(sql);
                int rows = 0;
                if (!st.execute()) // allows "with ... select ... update ..."
                {
                    rows = st.getUpdateCount();
                }
                ctx.report(ReportLevel.STATEMENT_STEP, "dynamic statement executed, changed rows: %d%n", rows);
            } finally {
                DbUtils.close(st);
            }
        }

    }

    private List<String> generateQueries(RunContext ctx, Connection connection, Map<String, byte[]> datafiles)
            throws SQLException {
        String sql = getStatement().trim();
        List<String> queries = new ArrayList<String>();
        PreparedStatement st = null;
        ResultSet rs = null;
        try {
            st = prepareStatement(connection, sql, datafiles, 0);
            int rowIdx = 0;
            rs = st.executeQuery();
            while (rs.next()) {
                ctx.report(ReportLevel.DETAIL, "reading result row #" + rowIdx);
                final String query = rs.getString(1);
                if (query != null) {
                    queries.add(query);
                }
                rowIdx++;
            }
            ctx.report(ReportLevel.STATEMENT_STEP, "generator statement executed, queries generated: %d%n", queries.size());
        } finally {
            DbUtils.close(st, rs);
        }
        return queries;
    }

}
