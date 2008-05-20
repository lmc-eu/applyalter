package ch.ips.g2.applyalter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.sql.SQLException;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.xml.sax.InputSource;

import ch.ips.base.BaseXMLUtil;


/**
 * Databases configuration with method applied to all (or used) database instances
 * @author Martin Caslavsky &lt;martin.caslavsky@ips-ag.cz&gt;
 * @version $Id$
 */
public class DbConfig
{
  protected Map<String, DbInstance> d;
  
  @SuppressWarnings("unchecked")
  public DbConfig(String file) {
    try {
      d = (Map<String, DbInstance>) BaseXMLUtil.fromXML(new InputSource(new FileInputStream(file)));
    } catch (FileNotFoundException e) {
      throw new ApplyAlterException("File not found " + file, e);
    }
    for (Map.Entry<String, DbInstance> i: d.entrySet())
      i.getValue().getConnection();
  }
  
  public Set<Entry<String, DbInstance>> getEntries() {
    return d.entrySet();
  }
 
  /**
   * Close connections to all database instances
   */
  public void closeConnections() {
    for (Map.Entry<String, DbInstance> i: d.entrySet())
      i.getValue().closeConnection();
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
    for (Map.Entry<String, DbInstance> i: d.entrySet()) {
      DbInstance b = i.getValue();
      if (b.isUsed())
        try {
          b.getConnection().commit();
        } catch (SQLException e) {
          ApplyAlterException ex = new ApplyAlterException("Error commiting", e);
          if (ignorefailures) aae.add(ex);
          else throw ex;
        }
    }
    if (!aae.isEmpty())
      throw aae;
  }

  public Set<String> getDbIds()
  {
    return d.keySet();
  }
  

  
}
