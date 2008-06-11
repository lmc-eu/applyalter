package ch.ips.g2.applyalter;

/**
 * @author Kamil Podlesak &lt;kamil.podlesak@ips-ag.cz&gt;
 * @version $Id$
 */
public abstract class AbstractMigration extends AbstractStatement
{
  protected String logid;
  protected String description;
  protected Long maxblkcnt;

  public String getLogid()
  {
    return logid;
  }

  public void setLogid(String logid)
  {
    this.logid = logid;
  }

  public String getDescription()
  {
    return description;
  }

  public void setDescription(String description)
  {
    this.description = description;
  }

  /**
   * Maximum number of blocks (if positive). Negative number or zero means that
   * amount of migrated data is unlimited.
   */
  public Long getMaxblkcnt()
  {
    return maxblkcnt;
  }

  /**
   * Maximum number of blocks (if positive). Negative number or zero means that
   * amount of migrated data is unlimited.
   */
  public void setMaxblkcnt(Long maxblkcnt)
  {
    this.maxblkcnt = maxblkcnt;
  }
  
}
