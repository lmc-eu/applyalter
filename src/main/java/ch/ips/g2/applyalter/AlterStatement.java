package ch.ips.g2.applyalter;

import java.sql.SQLException;
import java.util.Map;
import java.util.Set;


/**
 * Single Alter statement.
 *
 * @author Martin Caslavsky &lt;martin.caslavsky@ips-ag.cz&gt;
 * @version $Id$
 */
public interface AlterStatement {
    /**
     * The statement to execte/process; different statement types have different meanings of this value.
     */
    String getStatement();

    /**
     * Every statement must implement {@link #toString()} - it is used to print the statement to user.
     *
     * @return string representation of statement
     */
    String toString();

    void recordStructuredInfo(RunContext rctx);

    /**
     * Is the statement allowed to fail?
     *
     * @return true = after exception, alter script just continue; false = exception is fatal
     */
    boolean canFail();

    /**
     * Execute the statement. This method does only the real execution; database connection must be already
     * ready, used informed, etc. Method is not called if mode==print (in such case, it should do nothing
     * anyway).
     * Method should do basic clenup (closing of statements, etc) but error-handling is done by caller.
     *
     * @param dbConn    database connection (ready)
     * @param ctx       run context: contains run mode, provides reporting
     * @param datafiles data files
     * @throws SQLException        sql error, provides SQLSTATE and SQLCODE
     * @throws ApplyAlterException preprocessed exception
     */
    void execute(DbInstance dbConn, RunContext ctx, Map<String, byte[]> datafiles)
            throws SQLException, ApplyAlterException;


    /**
     * SQL states that can be safely ignored.
     *
     * @return set of ignored codes, might be null!
     */
    Set<String> getIgnoredSqlStates();

    /**
     * SQL codes that can be safely ignored.
     *
     * @return set of ignored codes, might be null!
     */
    Set<Integer> getIgnoredSqlCodes();
}
