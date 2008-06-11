package ch.ips.g2.applyalter;

public abstract class AbstractStatement implements AlterStatement
{
  public String statement;
  
  public boolean canfail;

  public String getStatement()
  {
    return statement;
  }

  public boolean canFail()
  {
    return canfail;
  }

  public void setCanfail(boolean canfail)
  {
    this.canfail = canfail;
  }

  public void setStatement(String statement)
  {
    this.statement = statement;
  }

/* Implementing hashCode+equals is not neccesary and probably incorrect

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
    //both objects must be the same class!
    if ( getClass() != obj.getClass() )
      return false;
    final AbstractStatement other = (AbstractStatement) obj;
    if( statement == null) {
      if( other.statement != null)
        return false;
    } else if( !statement.equals(other.statement))
      return false;
    return true;
  }
*/

  @Override
  public String toString()
  {
    return this.getClass().getSimpleName() + ": " + statement;
  }
  
  

}
