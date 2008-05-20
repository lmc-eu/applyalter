package ch.ips.g2.applyalter;

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
