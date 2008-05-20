package ch.ips.g2.applyalter;

public abstract class AbstractStatement implements AlterStatement
{
  public String statement;

  public String getStatement()
  {
    return statement;
  }

  public String getSQLStatement()
  {
    return statement;
  }

  public void setStatement(String statement)
  {
    this.statement = statement;
  }

  @Override
  public int hashCode()
  {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((statement == null) ? 0 : statement.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj)
  {
    if( this == obj)
      return true;
    if( obj == null)
      return false;
    if( !(obj instanceof AbstractStatement))
      return false;
    final AbstractStatement other = (AbstractStatement) obj;
    if( statement == null) {
      if( other.statement != null)
        return false;
    } else if( !statement.equals(other.statement))
      return false;
    return true;
  }

  @Override
  public String toString()
  {
    return statement;
  }
  
  

}
