package ch.ips.g2.applyalter;

import com.thoughtworks.xstream.annotations.XStreamAlias;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * DB2 JDBC app driver: requires native driver, but can skip the username and password (using the system
 * user authorization).
 *
 * @author Kamil Podlesak &lt;kamil.podlesak@ips-ag.cz&gt;
 * @version $Id$
 */
@XStreamAlias("db2native")
public class Db2Native extends Db2Instance
{
  //private static final String DB_DRIVER = "COM.ibm.db2.jdbc.app.DB2Driver";

  public String driver;

  public Db2Native()
  {
  }

  public Db2Native( String id, String type, String host, Integer port, String db, String user, String pass )
  {
    super( id, type, host, port, db, user, pass );
  }


  /**
   * Get url for connecting <code>jdbc:db2://...</code>
   *
   * @return url for connectiong
   */
  @Override
  public String getUrl()
  {
    return String.format( "jdbc:db2:%s", db );
  }

  /**
   * Connect the database: if {@link #user} is null or empty, the passwordless variant of
   * {@link DriverManager#getConnection(String)} is called. DB2 will then use system user.
   */
  @Override
  protected Connection connect( String url )
      throws SQLException
  {
    try
    {
      if ( driver != null )
      {
        Class.forName( driver );
      }
      else
      {
        Class.forName( Db2Instance.DB_DRIVER );
      }
    }
    catch (ClassNotFoundException e)
    {
      throw new ApplyAlterException( "Can not initialize db driver " + driver, e );
    }

    if ( user != null && user.length() > 0 )
    {
      //just used default user/password pair
      return DriverManager.getConnection( url, user, pass );
    }
    else
    {
      //use system user
      return DriverManager.getConnection( url );
    }

  }
}