package ch.ips.g2.applyalter;

import java.sql.Connection;

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
  public String schema;
  @XStreamAsAttribute
  public String name;
  
  public Check() {
    super();
  }
  public Check(CheckType type, String schema, String name) {
    this();
    this.type = type;
    this.schema = schema;
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
  public String getSchema()
  {
    return schema;
  }
  public void setSchema(String schema)
  {
    this.schema = schema;
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
    if (schema == null || "".equals(schema.trim()))
      throw new IllegalStateException("Schema have to be filled");
    if (name == null || "".equals(name.trim()))
      throw new IllegalStateException("Name have to be filled");
    if (type == null)
      throw new IllegalStateException("Name have to be filled");
  }
  
  @Override
  public String toString()
  {
    return String.format("%s %s.%s", type, schema, name);
  }

  public static void main(String[] args) throws Exception {
    Connection c = Dump.m.getConnection();
    System.out.println(ApplyAlter.check(c, Dump.ch));
  }
  
}
