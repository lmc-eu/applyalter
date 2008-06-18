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
 * @deprecated use {@link ch.ips.g2.applyalter.MigrationIdRange} or {@link ch.ips.g2.applyalter.MigrationIdList}
 * instead
 */
@XStreamAlias("migration")
public class MigrationProc extends AbstractMigration
{
  private Long fromid;
  private String fromidexpr;
  private Long toid;
  private String toidexpr;

  public MigrationProc() {
    super();
  }

  public MigrationProc(String logid, Long maxblkcnt, String description, String statement) {
    this();
    this.logid = logid;
    this.maxblkcnt = maxblkcnt;
    this.description = description;
    super.statement = statement;
  }

  public MigrationProc(String logid, Long maxblkcnt, String description,
      String statement, Long fromid, Long toid, Long step) {
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
  protected String getDefaultPlaceholder()
  {
    return MigrationIdRange.DEFAULT_PLACEHOLDER;
  }


  //-----------------------------------------------------------------------------------------------------------------
  /**
   * Helper method to resolve pairs {@link #toid}+{@link #toidexpr}, {@link #fromid}+{@link #fromidexpr}.
   * If the direct value is available, it is returned; otherwise, expression is evaluated via
   * {@link #evaluateIdExpression(java.sql.Connection, RunContext, String)}.
   *
   * @param con connection database connection
   * @param value direct value ({@link #fromid}, {@link #toid})
   * @param expression expression value ({@link #fromid}, {@link #toid})
   * @return the range value
   * @throws SQLException error evaluating the expression
   * @throws IllegalStateException both values are null
   */
  protected long fetchRangeValue( Connection con, RunContext ctx, Long value, String expression )
      throws SQLException
  {
    if ( value != null )
      return value.longValue();
    if ( expression != null )
    {
      return evaluateIdExpression( con, ctx, expression );
    }
    throw new ApplyAlterException( "invalid apply script: missing range value for " + this );
  }

  protected long evaluateIdExpression( Connection con, RunContext ctx, String expression )
      throws SQLException
  {
    String sql = "values " + expression;
    System.out.println( "  " + sql );

    PreparedStatement s = null;
    ResultSet rs = null;
    try
    {
      s = con.prepareStatement( sql );
      rs = s.executeQuery();
      if ( !rs.next() )
        throw new ApplyAlterException( "SQL statement toidexpr returned no result" );
      return rs.getLong( 1 );
    }
    finally
    {
      DbUtils.close( s, rs );
    }
  }

  //-----------------------------------------------------------------------------------------------------------------

  public String getSQLStatement()
  {
    return isFt() ? "call g2fn.blockupdate_ft(?,?,?,?,?,?,?)" : "call g2fn.blockupdate(?,?,?,?)";
  }


  protected PreparedStatement makePreparedStatement(Connection con, RunContext ctx)
      throws SQLException
  {
    PreparedStatement s = con.prepareStatement(getSQLStatement());
    int i = 1;
    s.setString(i++, logid);
    s.setLong(i++, maxblkcnt);
    s.setString(i++, description);
    s.setString(i++, statement);
    if (isFt()) {
      s.setLong(i++, fromid);
      Long ti = toidexpr != null ? evaluateIdExpression(con, ctx, toidexpr ) : toid;
      s.setLong(i++, ti);
      s.setLong(i++, step);
    }
    return s;
  }

  public void execute( DbInstance dbConn, RunContext mode )
      throws ApplyAlterException, SQLException
  {
    Connection connection = dbConn.getConnection();

    PreparedStatement st = null;
    try
    {
      st = makePreparedStatement( connection, mode );
      st.execute( );
    }
    finally
    {
      DbUtils.close( st );
    }

  }

  //-----------------------------------------------------------------------------------------------------------------
  //-----------------------------------------------------------------------------------------------------------------
  /**
   * Is this blockupdate_ft() or blockupdate()?
   * @return true if blockupdate_ft()
   * @throws IllegalArgumentException if any of required parameter is missing
   */
  public boolean isFt() throws IllegalArgumentException {
    if (logid != null && maxblkcnt != null && description != null && statement != null &&
        ( fromid != null || fromidexpr != null ) && (toid != null || toidexpr != null)
        && step != null)
      return true;
    else if (logid != null && maxblkcnt != null && description != null && statement != null)
      return false;
    else
      throw new IllegalArgumentException("Incorrectly filled parameters");
  }

  public Long getFromid()
  {
    return fromid;
  }

  public void setFromid(Long fromid)
  {
    this.fromid = fromid;
  }

  public Long getToid()
  {
    return toid;
  }

  public void setToid(Long toid)
  {
    this.toid = toid;
  }

  public String getFromidexpr()
  {
    return fromidexpr;
  }

  public void setFromidexpr( String fromidexpr )
  {
    this.fromidexpr = fromidexpr;
  }

  public String getToidexpr()
  {
    return toidexpr;
  }

  public void setToidexpr(String toidexpr)
  {
    this.toidexpr = toidexpr;
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
    b.append( "fromid: " ).append( fromid ).append( '/' ).append( toidexpr ).append( "\n" );
    b.append( "toid: " ).append( toid ).append( '/' ).append( toidexpr ).append( "\n" );
    b.append("step: ").append(step).append("\n");
    return b.toString();
  }
}
