package ch.ips.g2.applyalter;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Single Alter statement
 * @author Martin Caslavsky &lt;martin.caslavsky@ips-ag.cz&gt;
 * @version $Id$
 */
public interface AlterStatement
{
  public String getStatement();
  
  public String getSQLStatement();
  
  public PreparedStatement getPreparedStatement(Connection con) throws SQLException;
  
  public boolean canFail();
}
