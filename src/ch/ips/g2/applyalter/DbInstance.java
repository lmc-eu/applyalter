package ch.ips.g2.applyalter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;


/**
 * Database instance configuration. This class is not thread safe.
 * @author Martin Caslavsky &lt;martin.caslavsky@ips-ag.cz&gt;
 * @version $Id$
 */
public class DbInstance
{
  private static final String DB_DRIVER = "com.ibm.db2.jcc.DB2Driver";
  
  public String host;
  public Integer port;
  public String db;
  public String user;
  public String pass;
  private Connection con;
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
  public DbInstance(String host, Integer port, String db, String user, String pass) {
    this();
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
  public Connection useConnection() throws ApplyAlterException {
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
  @Override
  public int hashCode()
  {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((db == null) ? 0 : db.hashCode());
    result = prime * result + ((host == null) ? 0 : host.hashCode());
    result = prime * result + ((pass == null) ? 0 : pass.hashCode());
    result = prime * result + ((port == null) ? 0 : port.hashCode());
    result = prime * result + ((user == null) ? 0 : user.hashCode());
    return result;
  }
  @Override
  public boolean equals(Object obj)
  {
    if( this == obj)
      return true;
    if( obj == null)
      return false;
    if( !(obj instanceof DbInstance))
      return false;
    final DbInstance other = (DbInstance) obj;
    if( db == null) {
      if( other.db != null)
        return false;
    } else if( !db.equals(other.db))
      return false;
    if( host == null) {
      if( other.host != null)
        return false;
    } else if( !host.equals(other.host))
      return false;
    if( pass == null) {
      if( other.pass != null)
        return false;
    } else if( !pass.equals(other.pass))
      return false;
    if( port == null) {
      if( other.port != null)
        return false;
    } else if( !port.equals(other.port))
      return false;
    if( user == null) {
      if( other.user != null)
        return false;
    } else if( !user.equals(other.user))
      return false;
    return true;
  }
  
}
