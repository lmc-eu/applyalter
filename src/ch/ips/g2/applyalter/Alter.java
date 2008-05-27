package ch.ips.g2.applyalter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamImplicit;


/**
 * This class holds alter script (list of alter statements) which
 * are intended to be applied to a set of database instances or to all of them. 
 * Class is usually loaded from XML.
 * @author Martin Caslavsky &lt;martin.caslavsky@ips-ag.cz&gt;
 * @version $Id$
 */

@XStreamAlias("alter")
public class Alter
{
  public static final String DEFAULT_SCHEMA = "wasg2";
  private String id;
  public String schema = DEFAULT_SCHEMA;
  @XStreamAlias("checkok")
  public String checkok;
  @XStreamImplicit
  public List<Check> checks = new ArrayList<Check>();
  @XStreamImplicit
  public Set<DbInstanceType> instances = new HashSet<DbInstanceType>();
  @XStreamImplicit
  public List<AlterStatement> statements = new ArrayList<AlterStatement>();
  
  public Alter() {
    super();
  }
  public Alter(Set<DbInstanceType> instance, List<AlterStatement> statements) {
    this();
    this.instances = instance;
    this.statements = statements;
  }
  public Alter(Set<DbInstanceType> instances, String check, AlterStatement... statements) {
    this();
    this.instances = instances;
    this.checkok = check;
    addStatements(statements);
  }
  public Alter(DbInstanceType instance, String check, AlterStatement... statements) {
    this();
    this.instances.add(instance);
    this.checkok = check;
    addStatements(statements);
  }
  
  public void addStatements(AlterStatement... statements)
  {
    for (AlterStatement i: statements)
      this.statements.add(i);
  }
  public List<AlterStatement> getStatements()
  {
    return statements;
  }
  public String getSchema()
  {
    return schema;
  }
  public void setSchema(String schema)
  {
    this.schema = schema;
  }
  public void setStatements(List<AlterStatement> statements)
  {
    this.statements = statements;
  }
  public Set<DbInstanceType> getInstances()
  {
    return instances;
  }
  public void setInstances(Set<DbInstanceType> instances)
  {
    this.instances = instances;
  }
  /**
   * Apply this alter to all instances?
   * @return true if so, false otherwise
   */
  public boolean isAllInstances() {
    return instances == null || instances.isEmpty();
  }
  public String getId()
  {
    return id;
  }
  public void setId(String id)
  {
    this.id = id;
  }
  
  public String getCheckok()
  {
    return checkok;
  }
  public void setCheckok(String check)
  {
    this.checkok = check;
  }
  public List<Check> getChecks()
  {
    return checks;
  }
  public void setChecks(List<Check> checks)
  {
    this.checks = checks;
  }
  public void addChecks(Check...ch) {
    for (Check i: ch) {
      this.checks.add(i);
    }
  }

  private Object readResolve() {
    if (checks == null)
      checks = new ArrayList<Check>();
    if (statements == null)
      statements = new ArrayList<AlterStatement>();
    if (schema == null)
      schema = DEFAULT_SCHEMA;
    return this;
  }
  
}
