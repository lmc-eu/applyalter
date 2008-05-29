package ch.ips.g2.applyalter;

import com.thoughtworks.xstream.annotations.XStreamAlias;

/**
 * SQL statement in Alter script
 * @author Martin Caslavsky &lt;martin.caslavsky@ips-ag.cz&gt;
 * @version $Id$
 */
@XStreamAlias("sql")
public class SQL extends AbstractStatement
{

  public SQL() {
    super();
  }
  
  public SQL(String statement) {
    this();
    this.statement = statement;
  }

}
