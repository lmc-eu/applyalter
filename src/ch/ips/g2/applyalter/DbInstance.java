package ch.ips.g2.applyalter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.PreparedStatement;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamOmitField;


/**
 * Database instance configuration. This class is not thread safe.
 * @author Martin Caslavsky &lt;martin.caslavsky@ips-ag.cz&gt;
 * @version $Id$
 */
@XStreamAlias("dbinstance")
public class DbInstance
{
  private static final String DB_DRIVER = "com.ibm.db2.jcc.DB2Driver";
  
  public String id;
  public String type;
  public String host;
  public Integer port;
  public String db;
  public String user;
  public String pass;
  private Connection con;
  @XStreamOmitField
  private boolean used;
  
  static {
    try {
      Class.forName(DB_DRIVER);
    } catch (ClassNotFoundException e) {
      throw new ApplyAlterException("Can not initialize db driver " + DB_DRIVER, e);
    }
  }
  
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
  public String getUrl()
  {
    return String.format("jdbc:db2://%s:%s/%s", host, port, db);
  }
  
  /**
   * Change database schema on this databse instance
   * @param schema schema name to set
   * @throws ApplyAlterException if schema can not be set
   */
  public void setSchema(String schema) throws ApplyAlterException {
    //important: DB2 requires uppercase schema!
    schema = schema.toUpperCase();

    PreparedStatement ps = null;
    try {
      con.setCatalog(schema);

      ps = con.prepareStatement( "set schema ?" );
      ps.setString( 1, schema );
      ps.executeUpdate();

    } catch (SQLException e) {
      throw new ApplyAlterException("Can not set schema " + schema, e);
    } finally {
      DbUtils.close( ps );
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
        con = DriverManager.getConnection(url, user, pass);
        con.setAutoCommit(false);
      } catch (SQLException e) {
        throw new ApplyAlterException("Can not acquire db connection for " + url, e);
      }
    }
    return con;
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
      } catch (SQLException e) {}
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
  
}
