package ch.ips.g2.applyalter;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.thoughtworks.xstream.annotations.XStreamAlias;

/**
 * Migration script, which wouldnt fit to one transaction
 * database/alter2/b_7.1/nas_7.1/alter-common_fn.sql
 * database/alter2/b_7.3/nas_7.3.1/alter-common_fn.sql
 * 
 * @author Martin Caslavsky &lt;martin.caslavsky@ips-ag.cz&gt;
 * @version $Id$
 */
@XStreamAlias("migration")
public class Migration extends AbstractStatement
{
  
  public String logid;
  public Integer maxblkcnt;
  public String description;
  public Integer fromid;
  public Integer toid;
  public String toidexpr;
  public Integer step;

  public Migration() {
    super();
  }

  public Migration(String logid, Integer maxblkcnt, String description, String statement) {
    this();
    this.logid = logid;
    this.maxblkcnt = maxblkcnt;
    this.description = description;
    super.statement = statement;
  }

  public Migration(String logid, Integer maxblkcnt, String description,
      String statement, Integer fromid, Integer toid, Integer step) {
    super();
    this.logid = logid;
    this.maxblkcnt = maxblkcnt;
    this.description = description;
    super.statement = statement;
    this.fromid = fromid;
    this.toid = toid;
    this.step = step;
  }

  @Override
  public String getSQLStatement()
  {
    return isFt() ? "call g2fn.blockupdate_ft(?,?,?,?,?,?,?)" : "call g2fn.blockupdate(?,?,?,?)";
  }

  @Override
  public PreparedStatement getPreparedStatement(Connection con)
      throws SQLException
  {
    PreparedStatement s = con.prepareStatement(getSQLStatement());
    int i = 1;
    s.setString(i++, logid);
    s.setInt(i++, maxblkcnt);
    s.setString(i++, description);
    s.setString(i++, statement);
    if (isFt()) {
      s.setInt(i++, fromid);
      Integer ti = toidexpr != null ? getToidexpr(con) : toid;
      s.setInt(i++, ti);
      s.setInt(i++, step);      
    }
    return s;
  }
  
  private Integer getToidexpr(Connection con) throws SQLException
  {
    PreparedStatement s = con.prepareStatement(toidexpr);
    s.execute();
    ResultSet rs = s.getResultSet();
    if (!rs.next())
      throw new ApplyAlterException("SQL statement toidexpr returned no result");
    return rs.getInt(1);
  }

  /**
   * Is this blockupdate_ft() or blockupdate()?
   * @return true if blockupdate_ft()
   * @throws IllegalArgumentException if any of required parameter is missing
   */
  public boolean isFt() throws IllegalArgumentException {
    if (logid != null && maxblkcnt != null && description != null && statement != null && 
        fromid != null && (toid != null || toidexpr != null) && step != null)
      return true;
    else if (logid != null && maxblkcnt != null && description != null && statement != null)
      return false;
    else
      throw new IllegalArgumentException("Incorrectly filled parameters");
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

  public Integer getFromid()
  {
    return fromid;
  }

  public void setFromid(Integer fromid)
  {
    this.fromid = fromid;
  }

  public Integer getToid()
  {
    return toid;
  }

  public void setToid(Integer toid)
  {
    this.toid = toid;
  }

  public String getToidexpr()
  {
    return toidexpr;
  }

  public void setToidexpr(String toidexpr)
  {
    this.toidexpr = toidexpr;
  }

  public Integer getStep()
  {
    return step;
  }

  public void setStep(Integer step)
  {
    this.step = step;
  }

  @Override
  public String toString()
  {
    StringBuilder b = new StringBuilder();
    b.append(this.getClass().getSimpleName()).append(": ");
    b.append(getSQLStatement()).append(" ").append(statement).append("\n");
    b.append("logid: ").append(logid).append("\n");
    b.append("maxblkcnt: ").append(maxblkcnt).append("\n");
    b.append("description: ").append(description).append("\n");
    b.append("fromid: ").append(fromid).append("\n");
    b.append("toid: ").append(toid).append("\n");
    b.append("toidexpr: ").append(toidexpr).append("\n");
    b.append("step: ").append(step).append("\n");
    return b.toString();    
  }
}
