package ch.ips.g2.applyalter;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamImplicit;

import java.util.Set;
import java.util.Map;
import java.util.List;
import java.util.Collections;
import java.util.ArrayList;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.sql.PreparedStatement;
import java.sql.Connection;
import java.sql.SQLException;
import java.io.UnsupportedEncodingException;

public abstract class AbstractStatement implements AlterStatement
{
  protected String statement;

  /**
   * Any failure is acceptable and can be ignored.
   */
  protected boolean canfail;

  /**
   * SQL states that can be safely ignored.
   */
  @XStreamAlias("ignore-sqlstate")
  @XStreamImplicit
  protected Set<String> ignoredSqlStates;
  /**
   * Sql codes that can be safely ignored.
   */
  @XStreamAlias("ignore-sqlcode")
  @XStreamImplicit
  protected Set<Integer> ignoredSqlCodes;

  public String getStatement()
  {
    return statement;
  }

  public boolean canFail()
  {
    return canfail;
  }

  public void setCanfail( boolean canfail )
  {
    this.canfail = canfail;
  }

  public void setStatement( String statement )
  {
    this.statement = statement;
  }


  public Set<String> getIgnoredSqlStates()
  {
    return ignoredSqlStates;
  }

  public void setIgnoredSqlStates( Set<String> ignoredSqlStates )
  {
    this.ignoredSqlStates = ignoredSqlStates;
  }

  public Set<Integer> getIgnoredSqlCodes()
  {
    return ignoredSqlCodes;
  }

  public void setIgnoredSqlCodes( Set<Integer> ignoredSqlCodes )
  {
    this.ignoredSqlCodes = ignoredSqlCodes;
  }

  /* Implementing hashCode+equals is not neccesary and probably incorrect

    @Override
    public int hashCode()
    {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((statement == null) ? 0 : statement.hashCode());
      return result;
    }

    @Override
    public boolean equals(Object obj)
    {
      if( this == obj)
        return true;
      if( obj == null)
        return false;
      //both objects must be the same class!
      if ( getClass() != obj.getClass() )
        return false;
      final AbstractStatement other = (AbstractStatement) obj;
      if( statement == null) {
        if( other.statement != null)
          return false;
      } else if( !statement.equals(other.statement))
        return false;
      return true;
    }
  */

  @Override
  public String toString()
  {
    return this.getClass().getSimpleName() + ": " + statement;
  }

  //-------------------------------------------------------------------------------------------------------------------

  protected static final Pattern REGEX_PLACEHOLDER_DATAFILE = Pattern.compile(
      ":(blob|clob)\\(([^)\\s]+)\\)",
      Pattern.CASE_INSENSITIVE
  );

  /**
   * Prepare simple parameterless SQL statement, replacacing special placeholders by datafile LOBs.
   */
  protected PreparedStatement prepareStatement( Connection dbConn, String osql, Map<String, byte[]> datafiles )
      throws SQLException
  {
    if ( datafiles == null )
      return dbConn.prepareStatement( osql );

    Matcher m = REGEX_PLACEHOLDER_DATAFILE.matcher( osql );
    final String sql;
    final List<Object> params;
    if ( m.find() )
    {
      params = new ArrayList<Object>();
      StringBuffer sb = new StringBuffer();
      int lastAppend = 0;
      do
      {
        Object dataObject = getParamObj( datafiles, m.group( 1 ), m.group( 2 ) );
        if ( dataObject == null )
        {
          //just append without changes
          sb.append( osql, lastAppend, m.end() );
        }
        else
        {
          sb.append( osql, lastAppend, m.start() );
          sb.append( "?" );
          params.add( dataObject );
        }
        lastAppend = m.end();

      } while ( m.find() );
      sb.append( osql, lastAppend, osql.length() );
      sql = sb.toString();
    }
    else
    {
      //no placeholders
      sql = osql;
      params = Collections.emptyList();
    }

    PreparedStatement ps = dbConn.prepareStatement( sql );
    for ( int idx = 0; idx < params.size(); idx++ )
    {
      Object param = params.get( idx );
      ps.setObject( idx + 1, param );
    }
    return ps;
  }

  private Object getParamObj( Map<String, byte[]> datafiles, String paramType, String paramName )
  {
    Object dataObject;
    byte[] data = datafiles.get( paramName );
    if ( data == null )
    {
      return null;
    }
    //blob or clob?
    if ( paramType.toLowerCase().startsWith( "b" ) )
    {
      dataObject = data;
    }
    else
    {
      try
      {
        dataObject = new String( data, "UTF8" );
      }
      catch (UnsupportedEncodingException e)
      {
        throw new ApplyAlterException( e );
      }
    }
    return dataObject;
  }

}
