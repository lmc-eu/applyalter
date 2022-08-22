package ch.ips.g2.applyalter;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import org.apache.commons.lang3.StringUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.ParameterMetaData;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * SQL statement in Alter script
 *
 * @author Martin Caslavsky &lt;martin.caslavsky@ips-ag.cz&gt;
 * @version $Id$
 */
@XStreamAlias("csv")
public class CSV extends AbstractStatement {
    protected String file;
    protected Integer step = null;

    public String getFile() {
        return file;
    }

    public void setFile(String file) {
        this.file = file;
    }

    public Integer getStep() {
        return step;
    }

    public void setStep(Integer step) {
        this.step = step;
    }

    public CSV() {
        super();
    }


    /**
     * Prepared statement parameters meta info.
     */
    private static class ParamMeta {
        private final int typeId;
        private final String name;
        ;;

        ParamMeta(int typeId, String name) {
            this.typeId = typeId;
            this.name = name;
        }

        int getTypeId() {
            return typeId;
        }

        String getName() {
            return name;
        }
    }

    /**
     * Sql statement is just the statement itself.
     */
    public String getSqlStatement() {
        return getStatement();
    }

    public void execute(DbInstance dbConn, RunContext ctx, Map<String, byte[]> datafiles)
            throws ApplyAlterException, SQLException {
        Connection connection = dbConn.getConnection(ctx);
        String sql = getSqlStatement().trim();

        final byte[] rawFile = datafiles.get(getFile());
        if (rawFile == null) {
            throw new ApplyAlterException(String.format("missing top-level element: <datafile>%s</datafile>", getFile()));
        }

        CSVReader rdr = new CSVReader(new InputStreamReader(new ByteArrayInputStream(rawFile), StandardCharsets.UTF_8));

        PreparedStatement st = null;
        try {
            final String[] headRow = rdr.readNext();
            if (headRow == null) {
                throw new ApplyAlterException(String.format("missing top-level element: <datafile>%s</datafile>", getFile()));
            }
            final int numParams = headRow.length;
            ctx.report(ReportLevel.STATEMENT_STEP, "CSV columns:%s%n", Arrays.asList(headRow));

            //hack: any LOBs must be _after_ CSV columns
            st = prepareStatement(connection, sql, datafiles, numParams);

            // Warning: postgres jdbc driver 42.2.5 criples (in setTimestamp) the meta data, make copy of them!
            final ParameterMetaData paramTypesCripled = st.getParameterMetaData();
            if (paramTypesCripled.getParameterCount() != numParams) {
                throw new ApplyAlterException(String.format("invalid CSV: %d columns for %d query parameters",
                        numParams, paramTypesCripled.getParameterCount()));
            }
            List<ParamMeta> pTypes = new ArrayList<>();
            for (int idx = 1; idx <= numParams; idx++) {
                pTypes.add(new ParamMeta(paramTypesCripled.getParameterType(idx), paramTypesCripled.getParameterTypeName(idx)));
            }
            int rows = 0;
            int execCnt = 0;
            final Integer step = getStep();

            String[] row;
            while ((row = rdr.readNext()) != null) {
                //fill parameters
                for (int paramIdx = 1; paramIdx <= numParams; paramIdx++) {
                    String paramVal = row[paramIdx - 1];
                    fillParam(st, pTypes.get(paramIdx - 1), paramIdx, paramVal);
                }

                //execute
                if (!st.execute()) // allows "with ... select ... update ..."
                {
                    rows += st.getUpdateCount();
                }
                execCnt++;

                if (step != null && step > 0 && (execCnt % step) == 0) {
                    commitStep(ctx, connection);
                }

            }

            ctx.report(ReportLevel.STATEMENT_STEP, "statement executed %d times, changed rows: %d%n", execCnt, rows);
            rdr.close();
        } catch (IOException | CsvValidationException e) {
            throw new ApplyAlterException("error reading CSV file " + getFile(), e);
        } finally {
            DbUtils.close(st);
        }

    }

    /**
     * Check the value type, parse value from CSV and call appropriate setXXX method.
     */
    private void fillParam(PreparedStatement st, final ParamMeta meta, int paramIdx, String paramVal)
            throws SQLException {
        final int type = meta.getTypeId();
        //string types are special: empty string is not null!
        switch (type) {
            case Types.VARCHAR:
            case Types.CLOB:
            case Types.CHAR:
                st.setString(paramIdx, paramVal);
                return;
        }
        //in other cases, empty string is considered NULL
        if (StringUtils.isEmpty(paramVal)) {
            st.setNull(paramIdx, type);
            return;
        }

        switch (type) {
            case Types.INTEGER:
                st.setInt(paramIdx, Integer.parseInt(paramVal));
                break;
            case Types.BIGINT:
                st.setLong(paramIdx, Long.parseLong(paramVal));
                break;
            case Types.SMALLINT:
                st.setShort(paramIdx, Short.parseShort(paramVal));
                break;
            case Types.DECIMAL:
            case Types.DOUBLE:
                st.setDouble(paramIdx, Double.parseDouble(paramVal));
                break;
            case Types.FLOAT:
                st.setFloat(paramIdx, Float.parseFloat(paramVal));
                break;
            case Types.TIMESTAMP:
            case Types.OTHER:
                st.setTimestamp(paramIdx, getTimestamp(paramVal));
                break;
            case Types.BINARY:
                try {
                    byte[] b = paramVal.getBytes(StandardCharsets.UTF_8);
                    int sz = b.length;
                    st.setBinaryStream(paramIdx, new ByteArrayInputStream(b), sz);
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
                break;
            default:
                throw new ApplyAlterException("unsupported type in CSV: " + meta.getName());
        }
    }

    // 2017-10-10 12:00:00.000
    private Timestamp getTimestamp(String strValue) {
        try {
            return strValue == null ? null : Timestamp.valueOf(strValue);
        } catch (IllegalArgumentException ex) {
            LocalDateTime dt = LocalDateTime.parse(strValue, DateTimeFormatter.ofPattern("yyyy-MM-dd-HH.mm.ss.SSS000"));
            return Timestamp.valueOf(dt);
        }
    }
}
