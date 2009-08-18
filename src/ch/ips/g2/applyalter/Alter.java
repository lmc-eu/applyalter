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

  /**
   * Engine: when set, alterscript will be ignored for other engines.
   */
  public String engine;

  public String schema = DEFAULT_SCHEMA;
  public IsolationLevel isolation;
  public String checkok;
  @XStreamImplicit
  public List<Check> checks = new ArrayList<Check>();
  @XStreamImplicit
  public Set<String> instance = new HashSet<String>();
  @XStreamImplicit
  public List<AlterStatement> statements = new ArrayList<AlterStatement>();
  
  public Alter() {
    super();
  }
  public Alter(Set<String> instance, List<AlterStatement> statements) {
    this();
    this.instance = instance;
    this.statements = statements;
  }
  public Alter(Set<String> instances, String check, AlterStatement... statements) {
    this();
    this.instance = instances;
    this.checkok = check;
    addStatements(statements);
  }
  public Alter(String instance, String check, AlterStatement... statements) {
    this();
    this.instance.add(instance);
    this.checkok = check;
    addStatements(statements);
  }
  
  private void addStatements( AlterStatement... statements )
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
  public IsolationLevel getIsolation()
  {
    return isolation;
  }
  public void setIsolation( IsolationLevel isolation )
  {
    this.isolation = isolation;
  }
  public void setStatements(List<AlterStatement> statements)
  {
    this.statements = statements;
  }
  public Set<String> getInstance()
  {
    return instance;
  }
  public void setInstance(Set<String> instances)
  {
    this.instance = instances;
  }
  /**
   * Apply this alter to all instances?
   * @return true if so, false otherwise
   */
  public boolean isAllInstances() {
    return instance == null || instance.isEmpty();
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
