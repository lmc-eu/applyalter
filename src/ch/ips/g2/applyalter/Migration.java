package ch.ips.g2.applyalter;

/**
 * Migration script, which wouldnt fit to one transaction
 * database/alter2/b_7.1/nas_7.1/alter-common_fn.sql
 * 
 * @author Martin Caslavsky &lt;martin.caslavsky@ips-ag.cz&gt;
 * @version $Id$
 */
public class Migration extends AbstractStatement
{

  public Migration() {
    super();
  }
  
  public Migration(String statement) {
    this();
    this.statement = statement;
  }

  @Override
  public String getSQLStatement()
  {
    return String.format("g2fn.blockupdate('%s')", statement);
  }

}
