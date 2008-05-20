package ch.ips.g2.applyalter;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Migration script, which wouldnt fit to one transaction
 * database/alter2/b_7.1/nas_7.1/alter-common_fn.sql
 * 
 * @author Martin Caslavsky &lt;martin.caslavsky@ips-ag.cz&gt;
 * @version $Id$
 */
public class Migration extends AbstractStatement
{
  
  public String logid;
  public Integer maxblkcnt;
  public String description;

  public Migration() {
    super();
  }

  public Migration(String logid, Integer maxblkcnt, String description, String statement) {
    this();
    this.logid = logid;
    this.maxblkcnt = maxblkcnt;
    this.description = description;
    this.statement = statement;
  }

  @Override
  public String getSQLStatement()
  {
    return String.format("call g2fn.blockupdate(?,?,?,?)");
  }

  @Override
  public PreparedStatement getPreparedStatement(Connection con)
      throws SQLException
  {
    PreparedStatement s = con.prepareStatement(getSQLStatement());
    s.setString(1, logid);
    s.setInt(2, maxblkcnt);
    s.setString(3, description);
    s.setString(4, statement);
    return s;
  }

  public String getLogid()
  {
    return logid;
  }

  public void setLogid(String logid)
  {
    this.logid = logid;
  }

  public Integer getMaxblkcnt()
  {
    return maxblkcnt;
  }

  public void setMaxblkcnt(Integer maxblkcnt)
  {
    this.maxblkcnt = maxblkcnt;
  }

  public String getDescription()
  {
    return description;
  }

  public void setDescription(String description)
  {
    this.description = description;
  }

}
