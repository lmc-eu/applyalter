package ch.ips.g2.applyalter;

import com.thoughtworks.xstream.annotations.XStreamAlias;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Map;

/**
 * SQL statement in Alter script
 *
 * @author Martin Caslavsky &lt;martin.caslavsky@ips-ag.cz&gt;
 * @version $Id$
 */
@XStreamAlias("sql")
public class SQL extends AbstractStatement
{

  public SQL()
  {
    super();
  }

  public SQL( String statement )
  {
    this();
    this.statement = statement;
  }

  /**
   * Sql statement is just the statement itself.
   *
   * @return statement
   */
  public String getSqlStatement()
  {
    return getStatement();
  }

  public void execute( DbInstance dbConn, RunContext ctx, Map<String, byte[]> datafiles )
      throws ApplyAlterException, SQLException
  {
    Connection connection = dbConn.getConnection();
    String sql = getSqlStatement().trim();

    PreparedStatement st = null;
    try
    {
      st = prepareStatement( connection, sql, datafiles, 0 );
      int rows = 0;
      if( !st.execute() ) // allows "with ... select ... update ..."
      {
        rows = st.getUpdateCount();
      }
      ctx.report( ReportLevel.STATEMENT_STEP, "statement executed, changed rows: %d%n", rows );
    }
    finally
    {
      DbUtils.close( st );
    }

  }

}
