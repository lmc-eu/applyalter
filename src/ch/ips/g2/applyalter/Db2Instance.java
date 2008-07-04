package ch.ips.g2.applyalter;

import com.thoughtworks.xstream.annotations.XStreamAlias;

/**
 * Standard db2 instance.
 *
 * @author Kamil Podlesak &lt;kamil.podlesak@ips-ag.cz&gt;
 * @version $Id$
 */
@XStreamAlias("dbinstance")
public class Db2Instance extends DbInstance
{
  protected static final String DB_DRIVER = "com.ibm.db2.jcc.DB2Driver";

  static
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

  public Db2Instance()
  {
  }

  public Db2Instance( String id, String type, String host, Integer port, String db, String user, String pass )
  {
    super( id, type, host, port, db, user, pass );
  }


  /**
   * Get url for connecting <code>jdbc:db2://...</code>
   *
   * @return url for connectiong
   */
  public String getUrl()
  {
    return String.format( "jdbc:db2://%s:%s/%s", host, port, db );
  }

}
