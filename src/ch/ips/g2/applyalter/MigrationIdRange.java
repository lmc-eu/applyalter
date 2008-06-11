package ch.ips.g2.applyalter;

import static ch.ips.g2.applyalter.ReportLevel.STATEMENT_STEP;
import com.thoughtworks.xstream.annotations.XStreamAlias;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Special migration for data that cannot fit into one transaction. This variant use ID range steps:
 * upper and lower bound must be specified (either by number, or by SQL subselect expression)
 * and this value is then processed by specified step. Every step is made by replacing special placeholder
 * in query by "<code>BETWEEN x AND y</code>" fragment.
 * <p>
 * Important limitation of this migration is that the migrated data table must have single BIGINT primary key
 * column. It is not possible to migrate tables with composite keys, tables with non-integer keys or multiple
 * tables at once.
 *
 * @version $Id$
 */
@XStreamAlias("migration-id-range")
public class MigrationIdRange extends AbstractMigration
{
  public static final String DEFAULT_PLACEHOLDER = "BETWEEN_RANGE";
  /**
   * Replacement for {@link #DEFAULT_PLACEHOLDER} (not configurable).
   */
  protected static final String REPLACEMENT = "? and ?";

  private Long fromid;
  private String fromidexpr;
  private Long toid;
  private String toidexpr;
  private Long step;
  private String placeholder = DEFAULT_PLACEHOLDER;

  //-----------------------------------------------------------------------------------------------------------------
  public Long getFromid()
  {
    return fromid;
  }

  public void setFromid( Long fromid )
  {
    this.fromid = fromid;
  }

  public Long getToid()
  {
    return toid;
  }

  public void setToid( Long toid )
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

  public void setToidexpr( String toidexpr )
  {
    this.toidexpr = toidexpr;
  }

  public Long getStep()
  {
    return step;
  }

  public void setStep( Long step )
  {
    this.step = step;
  }

  public String getPlaceholder()
  {
    return placeholder;
  }

  public void setPlaceholder( String placeholder )
  {
    this.placeholder = placeholder;
  }

  //-----------------------------------------------------------------------------------------------------------------
  /**
   * Helper method to resolve pairs {@link #toid}+{@link #toidexpr}, {@link #fromid}+{@link #fromidexpr}.
   * If the direct value is available, it is returned; otherwise, expression is evaliated via
   * {@link #evaluateIdExpression(RunContext,java.sql.Connection, String)}.
   *
   * @param ctx run context
   * @param con connection database connection
   * @param value direct value ({@link #fromid}, {@link #toid})
   * @param expression expression value ({@link #fromid}, {@link #toid}) @return the range value
   * @throws java.sql.SQLException error evaluating the expression
   * @throws IllegalStateException both values are null
   */
  protected long fetchRangeValue( RunContext ctx, Connection con, Long value, String expression )
      throws SQLException
  {
    if ( value != null )
      return value.longValue();
    if ( expression != null )
    {
      return evaluateIdExpression( ctx, con, expression );
    }
    throw new ApplyAlterException( "invalid apply script: missing range value for " + this );
  }

  protected long evaluateIdExpression( RunContext ctx, Connection con, String expression )
      throws SQLException
  {
    String sql = "values " + expression;
    ctx.report( STATEMENT_STEP, "resolving range bound: %s", sql );

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
  //-----------------------------------------------------------------------------------------------------------------

  private static class ProcessedQuery
  {
    final String statement;
    final int numPairs;

    private ProcessedQuery( String statement, int numPairs )
    {
      this.statement = statement;
      this.numPairs = numPairs;
    }
  }

  /**
   * Propeare SQL query: replace all occurences of {@link #getPlaceholder()} by
   *
   * @return processed query
   */
  protected ProcessedQuery getProcessedQuery()
  {
    final String rawStatement = getStatement();
    final String placeholder = getPlaceholder() == null ? DEFAULT_PLACEHOLDER : getPlaceholder();

    try
    {
      Pattern pattern = Pattern.compile( placeholder );

      Matcher matcher = pattern.matcher( rawStatement );
      StringBuffer sb = new StringBuffer();
      int counter = 0;
      while ( matcher.find() )
      {
        matcher.appendReplacement( sb, REPLACEMENT );
        counter++;
      }
      matcher.appendTail( sb );
      return new ProcessedQuery( sb.toString(), counter );
    }
    catch (PatternSyntaxException e)
    {
      throw new ApplyAlterException( "invalid placeholder, not a valid regex: %s", placeholder );
    }
  }


  public void execute( DbInstance dbConn, RunContext ctx )
      throws ApplyAlterException
  {
    Connection connection = dbConn.getConnection();

    PreparedStatement st = null;
    try
    {
      //get ranges
      Long lower = fetchRangeValue( ctx, connection, getFromid(), getFromidexpr() );
      Long upper = fetchRangeValue( ctx, connection, getToid(), getToidexpr() );

      ProcessedQuery query = getProcessedQuery();
      if ( query.numPairs < 1 )
      {
        //this means that the query is just plain one!
        throw new ApplyAlterException( "invalid query (missing %s placeholder): %s", getPlaceholder(), getStatement() );
      }

      ctx.report( STATEMENT_STEP, "runnig query: %s", query.statement );
      st = connection.prepareStatement( query.statement );

      final Long step = getStep();
      ctx.report( STATEMENT_STEP, " with IDs in range %d-%d, step %d", lower, upper, step );

      long maxL = upper - step;
      long totalChangedRows = 0;
      for ( long currentL = lower; currentL < maxL; currentL += step )
      {
        final long currentH = currentL + step - 1;
        //ctx.report( ReportLevel.DETAIL, "  step: BETWEEN %d AND %d", currentL, currentH );

        //full the query
        int stIdx = 1;
        for ( int i = 0; i < query.numPairs; i++ )
        {
          st.setLong( stIdx++, currentL );
          st.setLong( stIdx++, currentH );
        }
        //execute
        final int changedRows = st.executeUpdate();
        ctx.report( ReportLevel.DETAIL, "  step %d-%d finished, %d rows changed", currentL, currentH, changedRows );
        //commit
        connection.commit();

        totalChangedRows += changedRows;
      }

      ctx.report( STATEMENT_STEP, " migration finished, total %d rows changed", totalChangedRows );
    }
    catch (SQLException e)
    {
      //just rethrow
      throw new ApplyAlterException( e.getMessage(), e );
    }
    finally
    {
      DbUtils.close( st );
    }

  }

  //-----------------------------------------------------------------------------------------------------------------
  @Override
  public String toString()
  {
    StringBuilder b = new StringBuilder();
    b.append( this.getClass().getSimpleName() ).append( ": " );
    b.append( "statement:" ).append( " " ).append( statement ).append( "\n" );
    b.append( "logid: " ).append( logid ).append( "\n" );
    b.append( "maxblkcnt: " ).append( maxblkcnt ).append( "\n" );
    b.append( "description: " ).append( description ).append( "\n" );
    b.append( "fromid: " ).append( fromid ).append( '/' ).append( toidexpr ).append( "\n" );
    b.append( "toid: " ).append( toid ).append( '/' ).append( toidexpr ).append( "\n" );
    b.append( "step: " ).append( step ).append( "\n" );
    return b.toString();
  }
}