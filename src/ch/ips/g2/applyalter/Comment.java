package ch.ips.g2.applyalter;

import com.thoughtworks.xstream.annotations.XStreamAlias;

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
