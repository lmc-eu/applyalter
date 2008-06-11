package ch.ips.g2.applyalter;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Some small database utilities.
 *
 * @author Kamil Podlesak &lt;kamil.podlesak@ips-ag.cz&gt;
 * @version $Id$
 */
public class DbUtils
{

  /**
   * Close statement, do nothing if it's not open.
   * No exception is thrown, ever; this method is thus safe to use inside finally clause.
   *
   * @param stmt statemenet to close, null is handled (nothing is done in such case)
   */
  public static void close( Statement stmt )
  {
    if ( stmt == null )
      return;
    try
    {
      stmt.close();
    }
    catch (SQLException e)
    {
      //ignore
    }
  }

  /**
   * Close result set and statement (any of them or both can be mmissing).
   * No exception is thrown, ever; this method is thus safe to use inside finally clause.
   *
   * @param stmt statemenet to close, null is handled (nothing is done in such case)
   * @param rs result set to close, null is handled (nothing is done in such case)
   */
  public static void close( Statement stmt, ResultSet rs )
  {
    if ( rs != null )
    {
      try
      {
        rs.close();
      }
      catch (SQLException e)
      {
        //ignore
      }
    }
    close( stmt );
  }

}
