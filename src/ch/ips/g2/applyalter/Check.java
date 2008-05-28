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
  
  public Check() {
    super();
  }
  public Check(CheckType type, String name) {
    this();
    this.type = type;
    this.name = name;
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
  
  public void check() {
    if (name == null || "".equals(name.trim()))
      throw new IllegalStateException("Name have to be filled");
    if (type == null)
      throw new IllegalStateException("Type have to be filled");
  }
  
  @Override
  public String toString()
  {
    return String.format("%s %s", type, name);
  }
  
}
