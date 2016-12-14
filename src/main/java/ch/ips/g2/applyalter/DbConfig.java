package ch.ips.g2.applyalter;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


/**
 * Databases configuration with method applied to all (or used) database instances.
 * Database istances with null {@link DbInstance#getHost()} are skipped in constructor and never included
 * in {@link #instances}; they are, however, inclused in {@link #knownDbTypes}.
 *
 * @author Martin Caslavsky &lt;martin.caslavsky@ips-ag.cz&gt;
 * @version $Id$
 */
@SuppressWarnings({"ThrowableInstanceNeverThrown"})
public class DbConfig {
    protected String environment;
    protected final List<DbInstance> instances;
    protected final Set<String> knownDbTypes;

    /**
     * fail with first exception or collect them and report at one
     */
    protected boolean ignorefailures;

    public DbConfig(DbConfigFile dcf, boolean ignorefailures, RunContext ctx) {
        this(dcf.instances, ignorefailures, ctx);
        this.environment = dcf.environment;
    }

    public DbConfig(List<DbInstance> configuredDatabases, boolean ignorefailures, RunContext ctx) {
        this.instances = new ArrayList<DbInstance>();
        this.knownDbTypes = new HashSet<String>();
        for (DbInstance instance : configuredDatabases) {
            if (instance.getType() != null) {
                knownDbTypes.add(instance.getType());
            }
            if (instance.isReal()) {
                instances.add(instance);
                instance.getConnection(ctx);
            }
        }
        this.ignorefailures = ignorefailures;
    }

    /**
     * Optional environment: any script with &lt;environment&gt; element will be executed only if this value
     * is specified and equal. Attempt to execute such script without specified environement is error.
     */
    public String getEnvironment() {
        return environment;
    }

    public List<DbInstance> getEntries() {
        return instances;
    }

    public boolean isIgnorefailures() {
        return ignorefailures;
    }

    /**
     * Close connections to all database instances
     */
    public void closeConnections() {
        for (DbInstance i : instances) {
            i.closeConnection();
        }
    }

    /**
     * Commit used connections
     *
     * @throws ApplyAlterException if one or some of connection can not be commited
     * @see DbInstance#getConnection(RunContext)
     * @see DbInstance#isUsed()
     */
    public void commitUsed(RunContext ctx) throws ApplyAlterException {
        commitRollbackUsed(ctx, true, "COMMIT");
    }

    /**
     * Rollback used connections
     *
     * @see DbInstance#getConnection(RunContext)
     * @see DbInstance#isUsed()
     */
    public void rollbackUsed(RunContext ctx) throws ApplyAlterException {
        commitRollbackUsed(ctx, false, "ROLLBACK");
    }

    private void commitRollbackUsed(RunContext ctx, boolean commit, String transactionEnd) {
        ApplyAlterExceptions aae = new ApplyAlterExceptions(ignorefailures);
        for (DbInstance i : instances) {
            if (i.isUsed())
                try {
                    ctx.reportProperty(ReportLevel.ALTER, "transaction", transactionEnd);
                    Connection connection = i.getConnection(ctx);

                    if (commit)
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
     *
     * @return database types
     */
    public Set<String> getDbTypes() {
        return knownDbTypes;
    }


}
