package ch.ips.g2.applyalter;

import com.thoughtworks.xstream.annotations.XStreamOmitField;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;


/**
 * Database instance configuration. This class is not thread safe.
 * Database instance can be "fake": just omit the <code>db</code> tag.
 *
 * @author Martin Caslavsky &lt;martin.caslavsky@ips-ag.cz&gt;
 * @version $Id$
 */
public abstract class DbInstance
{

  public String id;
  public String type;
  public String host;
  public Integer port;
  public String db;
  public String user;
  public String pass;
  @XStreamOmitField
  protected Connection con;
  @XStreamOmitField
  protected boolean used;

  public DbInstance() {
    super();
  }
  public DbInstance(String id, String type, String host, Integer port, String db, String user, String pass) {
    this();
    this.id = id;
    this.type = type;
    this.host = host;
    this.port = port;
    this.db = db;
    this.user = user;
    this.pass = pass;
  }

  /**
   * Get url for connecting <code>jdbc:db2://...</code>
   * @return url for connectiong
   */
  public abstract String getUrl();

  /**
   * Change database schema on this databse instance
   * @param schema schema name to set
   * @throws ApplyAlterException if schema can not be set
   */
  public void setSchema(String schema) throws ApplyAlterException {
    //important: DB2 requires uppercase schema!
    schema = schema.toUpperCase();

    try {
      con.setCatalog(schema);

      DbUtils.executeUpdate( con, "set schema ?", schema );

    } catch (SQLException e) {
      throw new ApplyAlterException("Can not set schema " + schema, e);
    }
  }

  /**
   * Set transaction isolation level.
   * Connection must be open.
   *
   * @param isolation isolation level; null = do not change
   * @throws ApplyAlterException error setting
   */
  public void setIsolation( IsolationLevel isolation )
      throws ApplyAlterException
  {
    if (isolation==null)
        return;

    try
    {
      con.setTransactionIsolation( isolation.getJdbcValue() );
    }
    catch (SQLException e)
    {
      throw new ApplyAlterException( "Cannot set isolation level to %s (%d)", e,
          isolation, isolation.getJdbcValue() );
    }
  }

  /**
   * Get a current connection to this database instance with auto commit turned off
   * @return connection to this database instance
   * @throws ApplyAlterException if connection could not be acquired
   */
  public Connection getConnection() throws ApplyAlterException {
    if (con == null) {
      String url = getUrl();
      try {
        con = connect( url );
        con.setAutoCommit(false);
      } catch (SQLException e) {
        throw new ApplyAlterException("Can not acquire db connection for " + url, e);
      }
    }
    return con;
  }

  /**
   * The real implementation of {@link #getConnection()}. Default implementation just calls
   * {@link DriverManager#getConnection(String, String, String)}, subclass can obtain the connection
   * by some other way.
   * @param url JDBC url, made by {@link #getUrl()}
   * @return new connection
   * @throws SQLException error getting connection
   */
  protected Connection connect( String url )
      throws SQLException
  {
    return DriverManager.getConnection(url, user, pass);
  }

  /**
   * Get a current connection to this database instance nad mark it as used
   * @return connection to this database instance
   * @throws ApplyAlterException if connection could not be acquired
   * @see #getConnection()
   */
  public Connection markConnectionUsed() throws ApplyAlterException {
    Connection c = getConnection();
    used = true;
    return c;
  }

  /**
   * Close connection
   */
  public void closeConnection() {
    if (con != null) {
      try {
        con.close();
      } catch (SQLException e) {
        //ignore
      }
      con = null;
    }
  }

  public String getId()
  {
    return id;
  }
  public void setId(String id)
  {
    this.id = id;
  }
  public String getType()
  {
    return type;
  }
  public void setType(String type)
  {
    this.type = type;
  }
  public boolean isUsed()
  {
    return used;
  }
  public String getHost()
  {
    return host;
  }
  public void setHost(String host)
  {
    this.host = host;
  }
  public Integer getPort()
  {
    return port;
  }
  public void setPort(Integer port)
  {
    this.port = port;
  }
  public String getDb()
  {
    return db;
  }
  public void setDb(String db)
  {
    this.db = db;
  }
  public String getUser()
  {
    return user;
  }
  public void setUser(String user)
  {
    this.user = user;
  }
  public String getPass()
  {
    return pass;
  }
  public void setPass(String pass)
  {
    this.pass = pass;
  }


  /**
   * Is this database connect valid, non-fake?
   *
   * @return true=real, false=fake
   */
  public boolean isReal()
  {
    return getDb() != null;
  }

}
