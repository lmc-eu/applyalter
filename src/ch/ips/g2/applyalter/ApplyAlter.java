package ch.ips.g2.applyalter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.UnrecognizedOptionException;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.XStreamException;
import static ch.ips.g2.applyalter.ReportLevel.MAIN;
import static ch.ips.g2.applyalter.ReportLevel.ALTER;
import static ch.ips.g2.applyalter.ReportLevel.DETAIL;


/**
 * Apply alter scripts to database instances
 * @author Martin Caslavsky &lt;martin.caslavsky@ips-ag.cz&gt;
 * @version $Id$
 */
public class ApplyAlter
{
  /**
   * Expected result of <code>&lt;checkok&gt;</code> statement
   */
  public static final String CHECK_OK = "OK";
  /**
   * Print stacktrace parameter name
   */
  public static final String PRINTSTACKTRACE = "p";
  /**
   * Run mode parameter name
   */
  public static final String RUN_MODE = "r";
  /**
   * Ignore failures parameter name
   */
  public static final String IGNORE_FAILURES = "i";
  /**
   * User name parameter name
   */
  public static final String USER_NAME = "u";

  /**
   * Suffix for zip file
   */
  public static final String ZIP_SUFFIX = ".zip";
  /**
   * Suffix for xml file with serialized Alter
   */
  public static final String XML_SUFFIX = ".xml";

  private static final String[] INTERNAL_SCRIPTS = {"applyalter_log.xml"};

  /**
   * Configuration of database instances
   */
  protected DbConfig db;
  /**
   * run mode
   */
  protected RunContext runContext;
  
  protected XStream xstream = new XStream();
  protected String username;
  protected Set<String> unapplied = new LinkedHashSet<String>();


  protected RunMode getRunMode()
  {
    return runContext.getRunMode();
  }

  /**
   * Create instance
   * @param dbconfigfile XML serialized {@link DbConfig} (database configuration)
   */
  @SuppressWarnings("unchecked")
  public ApplyAlter(String dbconfigfile, RunContext runContext, boolean ignorefailures, String username) {
    this.runContext = runContext;
    this.username = username;

    xstream.processAnnotations( new Class[]{
        Alter.class,
        SQL.class,
        Comment.class,
        MigrationProc.class,
        MigrationIdRange.class,
        MigrationIdList.class,
        Db2Instance.class,
        Db2Native.class
    } );
    xstream.alias("db", List.class);
    
    try {
      List<DbInstance> d = (List<DbInstance>) xstream.fromXML(new FileInputStream(dbconfigfile));
      db = new DbConfig(d, ignorefailures);
    } catch (FileNotFoundException e) {
      throw new ApplyAlterException("File not found " + dbconfigfile, e);
    } catch (XStreamException e) {
      throw new ApplyAlterException("Unable to deserialize DbConfig from file " + dbconfigfile, e);
    }
  }

  /**
   * Apply internal alterscripts.
   */
  protected void applyInternal()
  {
    final RunContext backupCtx = this.runContext;

    Alter[] internalAlters = new Alter[INTERNAL_SCRIPTS.length];
    for ( int i = 0; i < INTERNAL_SCRIPTS.length; i++ )
    {
      String alterName = INTERNAL_SCRIPTS[i];
      internalAlters[i] = newAlter( alterName, getClass().getResourceAsStream( alterName ) );
    }
    
    //nothing should be shown to user
    this.runContext = new InternalRunContext();
    //run, but don't close the connections!
    applyWithoutClosing( internalAlters );

    //restore the context
    this.runContext = backupCtx;
  }


  
  /**
   * Apply alter scripts (.xml/.zip) to all or selected database instances
   * @param alterFiles files with XML serialized alter scripts 
   * @throws ApplyAlterException if one of files is not .xml or .zip, or alter application fails
   * @see #apply(Alter...)
   */
  public void apply(String... alterFiles) throws ApplyAlterException {
    List<Alter> a = new ArrayList<Alter>(alterFiles.length);
    
    for (int i=0; i<alterFiles.length; i++) {
      String f = alterFiles[i];
      if (f.endsWith(XML_SUFFIX))
        a.add(fromFile(f));
      else if (f.endsWith(ZIP_SUFFIX))
        a.addAll(fromZip(f));
      else
        throw new ApplyAlterException("Unknown filetype " + f);
    }
    
    // actually apply them
    apply(a.toArray(new Alter[a.size()]));
  }

  /**
   * Create new instance from XML serialized form
   * @param file identifier for {@link Alter#setId(String)}
   * @param i input stream to read from
   * @return new instance
   */
  public Alter newAlter(String file, InputStream i)
  {
    Alter a = null;
    try {
      a = (Alter) xstream.fromXML(i);
    } catch (XStreamException e) {
      throw new ApplyAlterException("Unable to deserialize Alter from file " + file, e);
    }
    // get file name part
    a.setId(new File(file).getName());
    return a;
  }

  /**
   * Create Alter instance from XML serialized from file 
   * @param file XML serialized Alter
   * @return new Alter instance
   * @throws ApplyAlterException if file can not be found
   */
  protected Alter fromFile(String file) throws ApplyAlterException
  {
    try {
      FileInputStream i = new FileInputStream(file);
      return newAlter(file, i);
    } catch (FileNotFoundException e) {
      throw new ApplyAlterException("File not found " + file, e);
    }
  }

  /**
   * Create a list of Alter instances from XML serialized from files stored in .zip.
   * List is sorted using {@link ZipEntryNameComparator}.
   * @param zipfile zip file containing XML files
   * @return list of new Alter instances
   * @throws ApplyAlterException if error occurs during zip file processing
   */
  protected List<Alter> fromZip(String zipfile) {
    try {
      ZipFile z = new ZipFile(zipfile);
      Enumeration<? extends ZipEntry> e = z.entries();
      List<ZipEntry> l = new ArrayList<ZipEntry>();

      while (e.hasMoreElements()) {
        ZipEntry i = e.nextElement();
        if (i.isDirectory() || !i.getName().endsWith(ApplyAlter.XML_SUFFIX))
          continue;
        l.add(i);
      }
      
      List<Alter> a = new ArrayList<Alter>(z.size());
      ZipEntry[] t = l.toArray(new ZipEntry[l.size()]);
      Arrays.sort(t, new ZipEntryNameComparator());
      for (ZipEntry i: t) {
        String id = i.getName();
        InputStream s = z.getInputStream(i);
        a.add(newAlter(id, s));
      }

      return a;
    } catch (IOException e) {
      throw new ApplyAlterException("Error reading zip file " + zipfile, e);
    }
  }

  /**
   * Check if object exists in database, which means an alter was applied already. 
   * @param c Connection to database
   * @param a check object
   * @return true if object exists in database
   * @throws ApplyAlterException
   */
  protected boolean check(Connection c, Check a, String schema) throws ApplyAlterException
  {
    a.check();
    PreparedStatement s = null;
    ResultSet rs = null;
    try {
      final String sql = a.getType().getSQL();
      StringBuilder buf = new StringBuilder();
      buf.append( "Check: " ).append( sql ).append( " (" );

      s = c.prepareStatement(sql);
      int i = 1;
      schema = schema.toUpperCase();
      s.setString(i++, schema);
      buf.append( schema ).append( ' ' );
      if (a.table != null) {
        String table = a.getTable().toUpperCase();
        buf.append( table ).append( ' ' );
        s.setString(i++, table);
      }
      String name = a.getName().toUpperCase();
      buf.append(name);
      s.setString(i++, name);
      buf.append( ")" );

      runContext.report( ReportLevel.STATEMENT_STEP, "%s", buf );

      rs = s.executeQuery();
      boolean rawResult = rs.next();

      //XOR with the "isInverted" flag
      return rawResult ^ a.isInverted();
      
    } catch (SQLException e) {
      throw new ApplyAlterException("Can not check " + a, e);
    } finally {
      DbUtils.close( s, rs );
    }
  }

  /**
   * Custom check if an alter was applieds already. 
   * @param c Connection to database
   * @param sql custom SQL statement
   * @return true if sql is not null and result of sql statement is equal {@link #CHECK_OK} value
   * @throws ApplyAlterException
   */
  protected boolean check(Connection c, String sql) throws ApplyAlterException
  {
    if (sql == null || "".equals(sql.trim()))
      return false;
    PreparedStatement s = null;
    ResultSet rs = null;
    try {
      runContext.report( ReportLevel.STATEMENT_STEP, "Check: %s", sql);
      s = c.prepareStatement(sql);
      rs = s.executeQuery();
      if (!rs.next())
        return false;
      String check = rs.getString(1);
      return (CHECK_OK.equalsIgnoreCase(check));
    } catch (SQLException e) {
      throw new ApplyAlterException("Can not check " + sql, e);
    } finally {
      DbUtils.close( s, rs );
    }
  }

  /**
   * Check if all database types in alters are defined in database configuration
   * @param alters to check
   * @throws ApplyAlterException if there is an unknown database type
   */
  protected void checkDbIds(Alter... alters) throws ApplyAlterException
  {
    Set<String> types = db.getDbTypes();
    for (Alter a: alters) {
      if (a.getInstance() == null)
        continue;
      for (String i: a.getInstance())
        if (!types.contains(i))
          throw new ApplyAlterException("Unknown database type " + i + " in alter " + a.getId() + ". Possible values: " + types);
    }
  }

  /**
   * Apply alter scripts to all or selected database instances
   * @param alters alter scripts to apply
   * @throws ApplyAlterException if one of statements can not be executed
   */
  public void apply(Alter... alters) throws ApplyAlterException {
    try
    {
      applyWithoutClosing( alters );
    }
    finally
    {
      db.closeConnections();
    }
  }

  /**
   * Apply alter scripts to all or selected database instances
   * @param alters alter scripts to apply
   * @throws ApplyAlterException if one of statements can not be executed
   */
  public void applyWithoutClosing( Alter... alters )
      throws ApplyAlterException
  {
    ApplyAlterExceptions aae = new ApplyAlterExceptions( db.isIgnorefailures() );
    //initialize databases
    runContext.report( ALTER, "Executing %d alterscripts on %d database instances",
        alters.length, db.getEntries().size() );

    checkDbIds( alters );

    // for all alter scripts
    for ( Alter a : alters )
    {
      applySingleAlter( a, aae );
    }

    if ( !aae.isEmpty() ) throw aae;
  }

  private void applySingleAlter( Alter a, ApplyAlterExceptions aae )
  {
    runContext.report( ALTER, "alterscript: %s", a.getId() );
    // for all (or selected) databases
    for ( DbInstance d : db.getEntries() )
    {
      // apply to this instance?
      if ( a.isAllInstances() || a.getInstance().contains( d.getType() ) )
      {
        long start = System.currentTimeMillis();
        String dbid = d.getId();
        try
        {
          Connection c = d.getConnection();
          runContext.report( DETAIL, "Database instance %s %s, schema %s", dbid, d.getUrl(), a.getSchema() );
          d.setSchema( a.getSchema() );
          d.setIsolation( a.getIsolation() );

          // do checks
          if ( executeChecks( a, c ) )
          {
            //alter already applied
            runContext.report( ALTER, "Alter already applied, skipping" );
            continue;
          }

          if ( RunMode.LOOK.equals( getRunMode() )) 
          {
            runContext.report( MAIN, "Alter %s seems unapplied", a.getId());
            unapplied.add( a.getId() );
            continue;
          }
          d.markConnectionUsed();
          // for all alter statements
          for ( AlterStatement s : a.getStatements() )
          {
            //print to user
            runContext.report( ReportLevel.STATEMENT, "%s", s );
            if ( RunMode.PRINT.equals( getRunMode() ) )
              continue;

            executeStatement( d, s );
          }
          long time = System.currentTimeMillis() - start;
          savelog( c, dbid, a.getId(), time );

        }
        catch (ApplyAlterException e)
        {
          aae.addOrThrow( e );
        }
      }
    }
    // commit each alter on used databases
    if (aae.isEmpty() && RunMode.SHARP.equals(getRunMode()))
    {
      db.commitUsed( runContext );
    }
    else
    {
      db.rollbackUsed( runContext );
    }
  }

  /**
   * Execute statement and handle errors (ignoge if configured so).
   *
   * @param db database instance
   * @param s statement
   * @throws ApplyAlterException statement failed and the error is not configured to be ignored
   */
  private void executeStatement( DbInstance db, AlterStatement s )
      throws ApplyAlterException
  {
    try
    {
      s.execute( db, runContext );
    }
    catch (ApplyAlterException e)
    {
      if ( s.canFail() )
      {
        runContext.report( ReportLevel.ERROR, "statement failed, ignoring: %s", e.getMessage() );
      }
      else
        throw e;
    }
    catch (SQLException e)
    {
      if ( s.canFail() )
      {
        runContext.report( ReportLevel.ERROR, "statement failed, ignoring: %s", e.getMessage() );
      }
      else if ( s.getIgnoredSqlStates() != null && s.getIgnoredSqlStates().contains( e.getSQLState() ) )
      {
        runContext.report( ReportLevel.ERROR, "statement failed with SQLSTATE=%s, ignoring: %s",
            e.getSQLState(), e.getMessage() );
      }
      else if ( s.getIgnoredSqlCodes() != null && s.getIgnoredSqlCodes().contains( e.getErrorCode() ) )
      {
        runContext.report( ReportLevel.ERROR, "statement failed with SQLSTATE=%s, ignoring: %s",
            e.getSQLState(), e.getMessage() );
      }
      else
        throw new ApplyAlterException( e.getMessage(), e );
    }
  }

  protected boolean executeChecks( Alter alter, Connection connection )
  {
    if ( check( connection, alter.getCheckok() ) )
    {
      //checkOK is sufficient
      return true;
    }
    //all other checks must be OK to decide that alter has been already applied
    final List<Check> checks = alter.getChecks();
    if ( checks.isEmpty() )
    {
      //no checks --> not applied
      return false;
    }
    for ( Check i : checks )
    {
      if ( !check( connection, i, alter.getSchema() ) )
      {
        return false;
      }
    }
    return true;
  }

  /**
   * Logs succesful alter to stdout and applyalter_log table
   * @param c connection
   * @param dbid database id
   * @param id alter id
   * @param time alter duration
   */
  protected void savelog(Connection c, String dbid, String id, long time)
  {
    runContext.report( ALTER, "Alter %s on %s took %s ms", id, dbid, time);

    if ( runContext.getRunMode() != RunMode.SHARP )
    {
      //do not write to database
      return;
    }

    PreparedStatement s = null;
    try
    {
      s = c.prepareStatement("insert into wasg2.applyalter_log (username,id,duration) values (?,?,?)");
      s.setString(1, username);
      s.setString(2, id);
      s.setLong(3, time);
      s.executeUpdate();
    }
    catch (SQLException e)
    {
      runContext.report( ReportLevel.ERROR, "failed to insert applyalter_log record: %s", e.getMessage() );
    }
    finally
    {
      DbUtils.close( s );
    }
  }
  
  /**
   * Get ids of applied alters read from log table 
   * @param c database instance connection
   * @return id of alters applied in this database instance connection 
   * (non null empty list if problem occurs)   
   */
  public Set<String> getApplyAlterLog(Connection c) {
    Set<String> result = new HashSet<String>();
    PreparedStatement s = null;
    try
    {
      s = c.prepareStatement( "select distinct id from applyalter_log" );
      ResultSet r = s.executeQuery();
      while ( r.next() )
      {
        result.add( r.getString( 1 ) );
      }
    }
    catch (SQLException e)
    {
      runContext.report( ReportLevel.ERROR, "failed to read applyalter_log record: %s", e.getMessage() );
    }
    finally
    {
      DbUtils.close( s );
    }
    return result;
  }
  
  /**
   * Get list of unapplied alters (check failed, no record in applyalter_log)
   * @return concatenated list ready to print
   */
  public String getUnappliedAlters() {
    Set<String> un = new LinkedHashSet<String>(unapplied);
    for (DbInstance d : db.getEntries() ) {
      un.removeAll( getApplyAlterLog( d.getConnection() ) );
    }
    StringBuilder s = new StringBuilder();
    for (String i: un)
      s.append( i ).append( ' ' );
    return s.toString();
  }

  public static void main(String[] args)
  {
    Options o = new Options();
    o.addOption(IGNORE_FAILURES, false, "ignore failures");
    o.addOption(PRINTSTACKTRACE, false, "print stacktrace");
    o.addOption(RUN_MODE, true, "runmode, possible values: "+ Arrays.toString( RunMode.values() ) );
    o.addOption(USER_NAME, true, "user name");

    boolean ignfail = false;
    boolean printstacktrace = false;
    RunMode rnmd = RunMode.SHARP;
    
    try {
      CommandLineParser parser = new BasicParser();
      CommandLine cmd = parser.parse(o, args);
      
      String username = cmd.getOptionValue(USER_NAME);
      if (username == null || "".equals(username.trim()))
        username = System.getProperty("user.name");
      if (username == null || "".equals(username.trim()))
        throw new UnrecognizedOptionException("User name can not be determined, use parameter -" + USER_NAME);

      rnmd = RunMode.getRunMode(cmd.getOptionValue(RUN_MODE), rnmd);
      ignfail = Boolean.valueOf(cmd.hasOption(IGNORE_FAILURES));
      printstacktrace = Boolean.valueOf(cmd.hasOption(PRINTSTACKTRACE));

      String[] a = cmd.getArgs();
      if (a.length < 1)
        throw new UnrecognizedOptionException("Not enough parameters (dbconfig.xml alterscripts...)");
      
      // prepare arguments
      String[] param = new String[a.length-1];
      System.arraycopy(a, 1, param, 0, a.length-1);
      
      PrintWriterRunContext rctx = PrintWriterRunContext.createStdInstance();
      rctx.setRunMode( rnmd );

      // go
      rctx.report( MAIN, "ApplyAlter started" );
      rctx.report( MAIN, "run mode: %s", rnmd );
      rctx.report( MAIN, "ignore failures: %s", ignfail );
      rctx.report( MAIN, "print stacktrace: %s", printstacktrace );

      ApplyAlter applyAlter = new ApplyAlter( a[0], rctx, ignfail, username );
      applyAlter.applyInternal();
      applyAlter.apply(param);
      if ( RunMode.LOOK.equals( rnmd ) ) 
      {

        rctx.report( MAIN, "Unapplied alters: \n  %s", applyAlter.getUnappliedAlters() );
      }
      
    } catch (UnrecognizedOptionException e) {
      System.out.println(e.getMessage());
      new HelpFormatter().printHelp("applyalter [options] <dbconfig.xml> (alter.xml|alter.zip) ...", o, false);
    } catch (Throwable e) {
      if (e instanceof ApplyAlterException && (!printstacktrace || ignfail)) 
        ((ApplyAlterException)e).printMessages(System.err);
      else
        e.printStackTrace(System.err);
      System.exit(-1);
    }
  }

}
