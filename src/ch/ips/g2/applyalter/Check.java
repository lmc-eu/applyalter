package ch.ips.g2.applyalter;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;

/**
 * Check if object exists in database.
 * @author Martin Caslavsky &lt;martin.caslavsky@ips-ag.cz&gt;
 * @version $Id$
 */
@XStreamAlias("check")
public class Check
{
  @XStreamAsAttribute
  public CheckType type;
  @XStreamAsAttribute
  public String name;
  @XStreamAsAttribute
  public String table;
  @XStreamAsAttribute
  public boolean inverted;
  
  public Check() {
    super();
  }
  public Check(CheckType type, String name) {
    this();
    this.type = type;
    this.name = name;
  }
  public Check(CheckType type, String name, String table) {
    super();
    this.type = type;
    this.name = name;
    this.table = table;
  }
  public CheckType getType()
  {
    return type;
  }
  public void setType(CheckType type)
  {
    this.type = type;
  }
  public String getName()
  {
    return name;
  }
  public void setName(String name)
  {
    this.name = name;
  }
  public String getTable()
  {
    return table;
  }
  public void setTable(String table)
  {
    this.table = table;
  }

  public boolean isInverted()
  {
    return inverted;
  }

  public void setInverted( boolean inverted )
  {
    this.inverted = inverted;
  }

  public void check() {
    if (name == null || "".equals(name.trim()))
      throw new IllegalStateException("Name have to be filled");
    if (type == null)
      throw new IllegalStateException("Type have to be filled");
    if ((type.equals(CheckType.reference) || type.equals(CheckType.column)) && table == null)
      throw new IllegalStateException("Table have to be filled for reference and column");
  }
  
  @Override
  public String toString()
  {
    return String.format("%s %s", type, name);
  }
  
}
