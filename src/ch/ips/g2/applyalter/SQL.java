package ch.ips.g2.applyalter;

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
