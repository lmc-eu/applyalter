package ch.ips.g2.applyalter;
import java.sql.SQLException;
import java.sql.Connection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.ArrayList;


/**
 * Databases configuration with method applied to all (or used) database instances.
 * Database istances with null {@link DbInstance#getHost()} are skipped in constructor and never included
 * in {@link #instances}; they are, however, inclused in {@link #knownDbTypes}.
 *
 * @author Martin Caslavsky &lt;martin.caslavsky@ips-ag.cz&gt;
 * @version $Id$
 */
@SuppressWarnings({"ThrowableInstanceNeverThrown"})
public class DbConfig
{
  protected final List<DbInstance> instances;
  protected final Set<String> knownDbTypes;
  
  /**
   * fail with first exception or collect them and report at one 
   */
  protected boolean ignorefailures;

  @SuppressWarnings("unchecked")
  public DbConfig( List<DbInstance> configuredDatabases, boolean ignorefailures )
  {
    this.instances = new ArrayList<DbInstance>();
    this.knownDbTypes = new HashSet<String>();
    for ( DbInstance instance : configuredDatabases )
    {
      if ( instance.getType() != null )
      {
        knownDbTypes.add( instance.getType() );
      }
      if ( instance.getHost() != null )
      {
        instances.add( instance );
        instance.getConnection();
      }
    }
    this.ignorefailures = ignorefailures;
  }

  public List<DbInstance> getEntries() {
    return instances;
  }
 
  public boolean isIgnorefailures()
  {
    return ignorefailures;
  }

  /**
   * Close connections to all database instances
   */
  public void closeConnections() {
    for (DbInstance i: instances )
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
    for ( DbInstance i: instances ) {
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
    return knownDbTypes;
  }


}
