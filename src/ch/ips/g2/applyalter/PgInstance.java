package ch.ips.g2.applyalter;

import java.sql.SQLException;
import java.sql.Connection;

import com.thoughtworks.xstream.annotations.XStreamAlias;

/**
 * PostgreSQL instance with optional hostname and port
 *
 * @author mcaslavsky
 */
@XStreamAlias("pginstance")
public class PgInstance extends DbInstance
{
  protected static final String DB_DRIVER = "org.postgresql.Driver";
  
  public static final String ENGINE = "Postgresql";

  public String role;

  protected static void initDriver()
  {
    try
    {
      Class.forName( DB_DRIVER );
    }
    catch (ClassNotFoundException e)
    {
      throw new ApplyAlterException( "Can not initialize db driver " + DB_DRIVER, e );
    }
  }

  public PgInstance()
  {
    initDriver();
  }

  public PgInstance( String id, String type, String host, Integer port, String db, String user, String pass )
  {
    super( id, type, host, port, db, user, pass );
  }


  public String getRole()
  {
    return role;
  }

  public void setRole( String role )
  {
    this.role = role;
  }

  /**
   * Get url for connecting <code>jdbc:postgresql://...</code>
   *
   * @return url for connectiong
   */
  @Override
  public String getUrl()
  {
    StringBuilder b = new StringBuilder("jdbc:postgresql:");
    if (host != null) {
      b.append( "//" ).append( host );
      if (port != null)
        b.append( ":" ).append( port );
      b.append( "/" );
    }
    b.append( db );
    return b.toString();
  }

  @Override
  public String getEngine()
  {
    return ENGINE;
  }

  /**
   * Special flag for "canfail" and "ignore-sql{code|state}" support on postgresql.
   * On failure, postgresql marks transaction for rollback. When we need to ignore that error,
   * statement must be done inside savepoint.
   *
   * @return true postgresql needs the savepoint
   */
  @Override
  public boolean isSavepointNeededForIgnoredFailure()
  {
    return true;
  }

  @Override
  public void setSchema( String schema )
      throws ApplyAlterException
  {
    try {
      DbUtils.executeUpdate( con, "set search_path to " + schema + ",public" );
    } catch (SQLException e) {
      throw new ApplyAlterException("Can not set schema " + schema, e);
    }
  }

  @Override
  protected Connection connect( String url )
      throws SQLException
  {
    final Connection con = super.connect( url );
    if ( getRole() != null )
    {
      try
      {
        DbUtils.executeUpdate( con, "set role to " + getRole() );
      }
      catch (SQLException e)
      {
        throw new ApplyAlterException( "Can not set role to: " + getRole(), e );
      }
    }
    return con;
  }


  @Override
  public String getLogTable()
  {
    return "public.applyalter_log";
  }
}
