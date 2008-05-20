package ch.ips.g2.applyalter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.sql.SQLException;
import java.util.List;

import org.xml.sax.InputSource;

import ch.ips.base.BaseXMLUtil;


/**
 * Databases configuration with method applied to all (or used) database instances
 * @author Martin Caslavsky &lt;martin.caslavsky@ips-ag.cz&gt;
 * @version $Id$
 */
public class DbConfig
{
  protected List<DbInstance> d;
  
  @SuppressWarnings("unchecked")
  public DbConfig(String file) {
    try {
      d = (List<DbInstance>) BaseXMLUtil.fromXML(new InputSource(new FileInputStream(file)));
    } catch (FileNotFoundException e) {
      throw new ApplyAlterException("File not found " + file, e);
    }
    for (DbInstance i: d)
      i.getConnection();
  }
  
  public List<DbInstance> getEntries() {
    return d;
  }
 
  /**
   * Close connections to all database instances
   */
  public void closeConnections() {
    for (DbInstance i: d)
      i.closeConnection();
  }

  /**
   * Commit used connections
   * @param ignorefailures 
   * @see DbInstance#useConnection() 
   * @see DbInstance#isUsed() 
   * @throws ApplyAlterException if one or some of connection can not be commited
   */
  public void commitUsed(boolean ignorefailures) throws ApplyAlterException
  {
    ApplyAlterExceptions aae = new ApplyAlterExceptions();
    for (DbInstance i: d) {
      if (i.isUsed())
        try {
          System.out.println("Commiting " + i.getId());
          i.getConnection().commit();
        } catch (SQLException e) {
          ApplyAlterException ex = new ApplyAlterException("Error commiting", e);
          if (ignorefailures) aae.add(ex);
          else throw ex;
        }
    }
    if (!aae.isEmpty())
      throw aae;
  }

}
