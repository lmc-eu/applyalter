package ch.ips.g2.applyalter;

import com.thoughtworks.xstream.annotations.XStreamAlias;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

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

  public void execute( DbInstance dbConn, RunContext ctx )
      throws ApplyAlterException, SQLException
  {
    Connection connection = dbConn.getConnection();
    String sql = getSqlStatement().trim();

    PreparedStatement st = null;
    try
    {
      st = connection.prepareStatement( sql );
      int rows = st.executeUpdate();
      ctx.report( ReportLevel.STATEMENT_STEP, "statement executed, changed rows: %d%n", rows );
    }
    finally
    {
      DbUtils.close( st );
    }

  }

}
