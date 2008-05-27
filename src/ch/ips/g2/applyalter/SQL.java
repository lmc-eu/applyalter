package ch.ips.g2.applyalter;

import com.thoughtworks.xstream.annotations.XStreamAlias;

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
