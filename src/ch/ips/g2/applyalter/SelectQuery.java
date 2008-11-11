package ch.ips.g2.applyalter;

import com.thoughtworks.xstream.annotations.XStreamAlias;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * SQL select query in Alter script
 *
 * @author Pavel Tisnovsky
 */
@XStreamAlias("select")
public class SelectQuery extends AbstractStatement
{
  /**
   * Implicit constructor
   */
  public SelectQuery()
  {
    super();
  }

  /**
   * Constructor with SQL statement as parameter
   * @param statement
   */
  public SelectQuery( String statement )
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
    return this.getStatement();
  }

  public void execute( DbInstance dbConn, RunContext ctx )
  throws ApplyAlterException, SQLException
  {
    Connection connection = dbConn.getConnection();
    String sql = getSqlStatement().trim();
    PreparedStatement st = null;
    ResultSet rs = null;
    try
    {
      st = connection.prepareStatement( sql );
      int count = 0;
      rs = st.executeQuery();
      int columns = rs.getMetaData().getColumnCount();
      while ( rs.next() )
      {
        count++;
        StringBuffer out = new StringBuffer( "" );
        for ( int column = 1; column <= columns; column++ )
        {
          out.append( "" + rs.getString( column ) );
          if ( column < columns )
          {
            out.append( "," );
          }
        }
        ctx.report( ReportLevel.STATEMENT_STEP, "SQL SELECT result: {%s}", out.toString() );
      }
      ctx.report( ReportLevel.STATEMENT_STEP, "statement executed, selected records: %d%n", count );
    }
    finally
    {
      if ( rs != null )
      {
        rs.close();
      }
      DbUtils.close( st );
    }

  }
}
