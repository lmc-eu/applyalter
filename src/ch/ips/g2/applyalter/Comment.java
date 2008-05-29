package ch.ips.g2.applyalter;

import com.thoughtworks.xstream.annotations.XStreamAlias;

/**
 * Comment in alter script (just printed to an user)
 * @author Martin Caslavsky &lt;martin.caslavsky@ips-ag.cz&gt;
 * @version $Id$
 */
@XStreamAlias("comment")
public class Comment extends AbstractStatement
{

  public Comment() {
    super();
  }
  
  public Comment(String statement) {
    this();
    this.statement = statement;
  }

  @Override
  public String getSQLStatement()
  {
    return null;
  }

}
