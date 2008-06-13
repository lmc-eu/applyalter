package ch.ips.g2.applyalter;
import java.sql.SQLException;
import java.sql.Connection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


/**
 * Databases configuration with method applied to all (or used) database instances
 * @author Martin Caslavsky &lt;martin.caslavsky@ips-ag.cz&gt;
 * @version $Id$
 */
@SuppressWarnings({"ThrowableInstanceNeverThrown"})
public class DbConfig
{
  protected List<DbInstance> d;
  
  /**
   * fail with first exception or collect them and report at one 
   */
  protected boolean ignorefailures;
  
  @SuppressWarnings("unchecked")
  public DbConfig(List<DbInstance> d, boolean ignorefailures) {
    this.d = d;
    for (DbInstance i: d)
      i.getConnection();
    this.ignorefailures = ignorefailures;
  }
  
  public List<DbInstance> getEntries() {
    return d;
  }
 
  public boolean isIgnorefailures()
  {
    return ignorefailures;
  }

  /**
   * Close connections to all database instances
   */
  public void closeConnections() {
    for (DbInstance i: d)
    {
      i.closeConnection();
    }
  }

  /**
   * Commit used connections
   * @see DbInstance#markConnectionUsed()
   * @see DbInstance#isUsed() 
   * @throws ApplyAlterException if one or some of connection can not be commited
   */
  public void commitUsed(RunContext ctx) throws ApplyAlterException
  {
    commitRollbackUsed( ctx, true, "Commiting %s" );
  }

  /**
   * Rollback used connections
   * @see DbInstance#markConnectionUsed()
   * @see DbInstance#isUsed()
   */
  public void rollbackUsed(RunContext ctx) throws ApplyAlterException
  {
    commitRollbackUsed( ctx, false, "Rolling back %s" );
  }

  private void commitRollbackUsed( RunContext ctx, boolean commit, String msgFormat )
  {
    ApplyAlterExceptions aae = new ApplyAlterExceptions(ignorefailures);
    for ( DbInstance i: d) {
      if (i.isUsed())
        try {
          ctx.report( ReportLevel.ALTER, msgFormat, i.getId() );
          Connection connection = i.getConnection();

          if ( commit )
            connection.commit();
          else
            connection.rollback();

        } catch (SQLException e) {
          aae.addOrThrow(new ApplyAlterException("Error commiting", e));
        }
    }
    if (!aae.isEmpty())
      throw aae;
  }

  /**
   * Get all DbInstance types
   * @return database types
   */
  public Set<String> getDbTypes()
  {
    Set<String> result = new HashSet<String>(d.size());
    for (DbInstance i: d) 
      result.add(i.getType());
    return result;
  }


}
