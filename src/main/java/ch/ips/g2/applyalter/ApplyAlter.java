package ch.ips.g2.applyalter;

import static ch.ips.g2.applyalter.ReportLevel.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.UnrecognizedOptionException;
import org.apache.commons.io.IOUtils;
import org.xml.sax.SAXException;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.XStreamException;


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
   * Incremental mode parameter name
   */
  public static final String INC_MODE = "s";
  /**
   * Ignore failures parameter name
   */
  public static final String IGNORE_FAILURES = "i";
  /**
   * User name parameter name
   */
  public static final String USER_NAME = "u";
  /**
   * Environment.
   */
  public static final String ENVIRONMENT_OPT = "e";
  /**
   * XML validation on/off
   */
  public static final String NO_VALIDATE_XML = "n";
  /**
   * Ignore failures parameter name
   */
  public static final String NO_LOG_TABLE = "L";
  /**
   * Suffix for zip file
   */
  public static final String ZIP_SUFFIX = ".zip";
  /**
   * Suffix for xml file with serialized Alter
   */
  public static final String XML_SUFFIX = ".xml";

  private static final String[] INTERNAL_SCRIPTS = {
          "applyalter_log_db2.xml", "applyalter_log_pgsql.xml",
          "applyalter_hash_db2.xml", "applyalter_hash_pgsql.xml",
          "applyalter_idindex_db2.xml", "applyalter_idindex_pgsql.xml",
  };

  /**
   * XML validator
   */
  private Validator validator = null;
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
  protected Multimap<String,String> unapplied = ArrayListMultimap.create();
  private boolean logTableUsed;
  private String environment;


  protected RunMode getRunMode()
  {
    return runContext.getRunMode();
  }

  public boolean isLogTableUsed()
  {
    return logTableUsed;
  }

  public void setLogTableUsed( boolean logTableUsed )
  {
    this.logTableUsed = logTableUsed;
  }

  /**
   * Get configured environment.
   * @return environment; null if no envirnment is configured
   */
  public String getEnvironment()
  {
    return environment;
  }

  /**
   * Create instance of ApplyAlter.
   *
   * @param dbconfigfile XML serialized {@link DbConfig} (database configuration)
   * @param runContext execution context, providing methods to output the results and report the processing steps.
   * @param ignorefailures
   * @param username
   * @param validateXml
   */
  @SuppressWarnings("unchecked")
  public ApplyAlter( String dbconfigfile, RunContext runContext, boolean ignorefailures, String username,
      boolean validateXml, boolean useLogTable, String environment )
  {
    this.runContext = runContext;
    this.username = username;
    this.setLogTableUsed( useLogTable );
    this.environment = environment;

    xstream.processAnnotations( getXmlClasses() );

    FileInputStream fis = null;
    try
    {
      fis = new FileInputStream( dbconfigfile );
      DbConfigFile dcf = (DbConfigFile) xstream.fromXML( fis );
      db = new DbConfig( dcf, ignorefailures );
      if ( validateXml )
      {
        this.validator = readXsd( runContext );
      }
    }
    catch ( FileNotFoundException e )
    {
      throw new ApplyAlterException( "File not found " + dbconfigfile, e );
    }
    catch ( XStreamException e )
    {
      throw new ApplyAlterException( "Unable to deserialize DbConfig from file " + dbconfigfile, e );
    }
    catch ( SAXException e )
    {
      throw new ApplyAlterException( "Unable to initialize XML validator", e );
    }
    finally
    {
      IOUtils.closeQuietly( fis );
//      BaseUtil.closeNoThrow( fis, "ApplyAlter" );
    }

    if ( this.environment == null )
    {
      this.environment = this.db.getEnvironment();
    }
    runContext.report( MAIN, "environment: %s", this.environment );
  }

  @SuppressWarnings({"deprecation"})
  private Class[] getXmlClasses()
  {
    return new Class[]{
        DbConfigFile.class,
        Alter.class,
        SQL.class,
        CSV.class,
        SelectQuery.class,
        DynamicQuery.class,
        Comment.class,
        MigrationProc.class,
        MigrationIdRange.class,
        MigrationIdList.class,
        PgInstance.class,
        OracleInstance.class,
        Db2Instance.class,
        Db2Native.class
    };
  }

  /**
   * Read XSD file applyalter.xsd and construct validator.
   *
   * @param runContext execution context, providing methods to output the results and report the processing steps.
   * @throws SAXException
   */
  private Validator readXsd( RunContext runContext )
      throws SAXException
  {
    // 1. Lookup a factory for the W3C XML Schema language
    SchemaFactory factory = SchemaFactory.newInstance( "http://www.w3.org/2001/XMLSchema" );

    // 2. Compile the schema.
    Schema schema = compileXMLSchema( runContext, factory );

    // 3. Get a validator from the schema and return it.
    return schema.newValidator();
  }

  /**
   * Read XSD file and compile XML schema.
   *
   * @param runContext execution context, providing methods to output the results and report the processing steps.
   * @param factory factory that creates Schema objects. Entry-point to the validation API.
   * @return Immutable in-memory representation of XML grammar.
   * @throws SAXException
   */
  private Schema compileXMLSchema( RunContext runContext, SchemaFactory factory )
      throws SAXException
  {
    Schema schema = null;

    try
    {
      File schemaLocation = new File( "applyalter.xsd" );
      schema = factory.newSchema( schemaLocation );
      if ( schema != null )
      {
        runContext.report( MAIN, "applyalter.xsd successfuly read from file" );
      }
    }
    catch ( Exception e )
    {
      //ignore exception
      runContext.report( MAIN, "Cannot read applyalter.xsd from file, trying read it from applyalter.jar" );
    }
    if ( schema == null )
    {
      StreamSource inputSource = new StreamSource( ApplyAlter.class.getResourceAsStream( "/applyalter.xsd" ) );
      schema = factory.newSchema( inputSource );
      if ( schema != null )
      {
        runContext.report( MAIN, "applyalter.xsd read successfuly read from applyalter.jar" );
      }
    }
    return schema;
  }

  /**
   * Apply internal alterscripts.
   */
  protected void applyInternal()
  {
    if ( !isLogTableUsed() )
    {
      //no internal scripts needed
      return;
    }

    final RunContext backupCtx = this.runContext;
    AlterLoader alterLoader = new AlterLoader( xstream, validator );

    Alter[] internalAlters = new Alter[INTERNAL_SCRIPTS.length];
    for ( int i = 0; i < INTERNAL_SCRIPTS.length; i++ )
    {
      final String alterName = INTERNAL_SCRIPTS[i];
      internalAlters[i] = alterLoader.parseScriptFile( alterName,
          new AlterLoader.RelativeToClassAlterSource( getClass(), alterName ) );
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
   */
  public void apply( boolean validateXml, String... alterFiles )
      throws ApplyAlterException
  {
    AlterLoader ldr = new AlterLoader( xstream, validator );
    List<Alter> a = ldr.loadAlters( alterFiles );
    // actually apply them
    apply( a.toArray( new Alter[a.size()] ) );
  }

  /**
   * Check if object exists in database, which means an alter was applied already.
   * @param d
   *@param c Connection to database
   * @param a check object @return true if object exists in database
   * @throws ApplyAlterException
   */
  protected boolean check( DbInstance d, Connection c, Check a, String schema ) throws ApplyAlterException
  {
    a.check();
    try {
      return d.check( runContext, c, a, schema );
    } catch (SQLException e) {
      throw new ApplyAlterException("Can not check " + a, e);
    } catch (UnsupportedOperationException e) {
      throw new ApplyAlterException("Unsupported check " + a, e);
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
   * Check if all database types (and environments) in alters are defined in database configuration.
   * If any alterscript contains unknown database it, exception is thrown.
   * If any alterscript contains environment and database configuration has none, exception is throws.
   *
   * @param alters to check
   * @throws ApplyAlterException if there is an unknown database type
   */
  protected void checkDbIds(Alter... alters) throws ApplyAlterException
  {
    Set<String> types = db.getDbTypes();
    Set<String> environments = new TreeSet<String>();
    for ( Alter a : alters )
    {
      if ( a.getInstance() == null )
      {
        continue;
      }
      for ( String i : a.getInstance() )
      {
        if ( !types.contains( i ) )
        {
          throw new ApplyAlterException( "Unknown database type " + i + " in alter " + a.getId() + ". Possible values: "
              + types );
        }
      }
      if ( a.environment != null )
      {
        environments.addAll( a.environment );
      }
    }
    if ( !environments.isEmpty() && getEnvironment() == null )
    {
      throw new ApplyAlterException( "No environment in confiuration, but alterscripts contain "
          + environments + ". Add environment to configuration." );
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
      //check engine
      if ( a.engine != null && !a.engine.equalsIgnoreCase( d.getEngine() ) )
      {
        //skip
        runContext.report( ALTER, "alterscript is only for %s, database is %s, skipping", a.engine, d.getEngine() );
        continue;
      }

      if ( a.environment != null && !a.environment.contains( db.getEnvironment() ) )
      {
        //skip
        runContext.report( ALTER, "alterscript is for environment %s, database is %s, skipping",
            a.environment, db.getEnvironment() );
        continue;
      }

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
          if ( executeChecks( a, d, c ) )
          {
            //alter already applied
            runContext.report( ALTER, "Alter already applied, skipping" );
            continue;
          }

          if ( RunMode.LOOK.equals( getRunMode() ))
          {
            runContext.report( MAIN, "Alter %s seems unapplied", a.getId());
            unapplied.put(d.getId(), a.getId() );
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

            executeStatement( d, a, s );
          }
          long time = System.currentTimeMillis() - start;
          savelog( d, dbid, a.getId(), time, a.getHash() );

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
   * @param a the alterscript
   * @param s statement  @throws ApplyAlterException statement failed and the error is not configured to be ignored
   */
  private void executeStatement( DbInstance db, Alter a, AlterStatement s )
      throws ApplyAlterException
  {
    Savepoint savepoint = null;
    if ( db.isSavepointNeededForIgnoredFailure() &&
        ( s.canFail() || s.getIgnoredSqlStates() != null || s.getIgnoredSqlCodes() != null ) )
    {
      try
      {
        savepoint = db.getConnection().setSavepoint(  );
      }
      catch (SQLException e)
      {
        throw new ApplyAlterException( e.getMessage(), e );
      }
    }

    try
    {
      s.execute( db, runContext, a._datafiles );
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

      //ok, error ignored; rollback to savepoint
      if ( savepoint != null )
      {
        try
        {
          db.getConnection().rollback( savepoint );
        }
        catch (SQLException e1)
        {
          //ignore e1
          throw new ApplyAlterException( e.getMessage(), e );
        }
      }
    }
  }

  protected boolean executeChecks( Alter alter, DbInstance d, Connection connection )
  {
    if (checkInc(alter, d, connection)) {
      return true;
    }
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
      if ( !check( d, connection, i, alter.getSchema() ) )
      {
        return false;
      }
    }
    return true;
  }

  /**
   * Check incremental mode.
   * @return true = this script has already been executed, skip it; <br />
   * false = execute it
   */
  boolean checkInc(Alter alter, DbInstance d, Connection c) {
    if (!isIncrimental) {
      // continue with checks
      return false;
    }
    PreparedStatement s = null;
    try {
      s = c.prepareStatement("select hash,AT from " + d.getLogTable() + " where id = ? order by AT desc");
      s.setString(1, alter.getId());
      s.setMaxRows( 1 );
      s.execute();
      ResultSet rs = s.getResultSet();
      //read only one hash (the last one)
      if (rs.next()) {
        String hash = rs.getString( 1 );
        Timestamp at = rs.getTimestamp( 2 );
        if (hash == null || !hash.equals(alter.getHash())) {
          runContext.report( ReportLevel.ALTER, String.format(
              "script id: %s changed since: %s (%s to %s)",
              alter.getId(), at.toString(), hash, alter.getHash()
          ) );
          //this script has been already executed, but with different hash!
          if (alter.synchronization) {
            // synchronization script: it is supposed to change
            //  no error, but the script is executed
            return false;
          }
          //normal script: it should not change!
          // log error and skip this script
          runContext.report(ReportLevel.ALTER, "hash doesn't match! " + hash + " is in DB for id: " + alter.getId()
              + " while script hash is: " + alter.getHash());
        }
        // the only case to skip script if option is set, sync is not set and result set is not empty
        runContext
            .report(ReportLevel.ALTER, "skipping script id: " + alter.getId() + " script hash: " + alter.getHash());
        return true;
      }
      runContext.report(ReportLevel.ALTER, "The first try script id: " + alter.getId());
    } catch (SQLException e) {
      runContext.report(ReportLevel.ERROR, "failed to select applyalter_log record: %s", e.getMessage());
    } finally {
      DbUtils.close(s);
    }
    // continue with checks
    return false;
  }

  /**
   * Logs successful alter to stdout and applyalter_log table
   * @param d database instance
   * @param dbid database id
   * @param id alter id
   * @param time alter duration
   */
  protected void savelog( DbInstance d, String dbid, String id, long time, String hash)
  {
    Connection c = d.getConnection();
    runContext.report( ALTER, "Alter %s on %s took %s ms", id, dbid, time);

    if ( runContext.getRunMode() != RunMode.SHARP || !isLogTableUsed() )
    {
      //do not write to database
      return;
    }

    PreparedStatement s = null;
    try
    {
      s = c.prepareStatement("insert into "+d.getLogTable()+" (username,id,duration,hash) values (?,?,?,?)");
      s.setString(1, username);
      s.setString(2, id);
      s.setLong(3, time);
      s.setString(4, hash);
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
  public Set<String> getApplyAlterLog( DbInstance d ) {
    Set<String> result = new HashSet<String>();
    Connection c = d.getConnection();
    PreparedStatement s = null;
    ResultSet r = null;
    try
    {
      s = c.prepareStatement( "select distinct id from "+d.getLogTable() );
      r = s.executeQuery();
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
      DbUtils.close( s, r );
    }
    return result;
  }

  /**
   * Get list of unapplied alters (check failed, no record in applyalter_log)
   * @return concatenated list ready to print
   */
  public String getUnappliedAlters() {
    Multimap<String,String> un = ArrayListMultimap.create(unapplied);
    StringBuilder s = new StringBuilder();
    for (DbInstance d : db.getEntries() ) {
      Collection<String> c = un.get( d.getId() );
      c.removeAll( getApplyAlterLog( d ) );
      if ( !c.isEmpty() ) {
        s.append( d.getId() ).append( ": " );
        for (String i: c)
          s.append( i ).append( ' ' );
        s.append( "\n" );
      }
    }
    return s.toString();
  }

  static boolean isIncrimental = false;
  /**
   * Main function, which can be called from command line.
   *
   * @param args
   */
  public static void main(String[] args)
 {
    Options o = new Options();
    o.addOption( IGNORE_FAILURES, false, "ignore failures" );
    o.addOption( PRINTSTACKTRACE, false, "print stacktrace" );
    o.addOption( RUN_MODE, true, "runmode, possible values: " + Arrays.toString( RunMode.values() ) );
    o.addOption( USER_NAME, true, "user name" );
    o.addOption( ENVIRONMENT_OPT, true, "environment" );
    o.addOption( NO_VALIDATE_XML, false, "disables XML file with alter script validation" );
    o.addOption( NO_LOG_TABLE, false, "disables log table" );
    o.addOption(INC_MODE, false, "incremental mode");
    o.addOption( "V", "version", false, "version" );

    boolean ignfail = false;
    boolean printstacktrace = false;
    boolean validateXml = true;
    boolean useLogTable;
    RunMode rnmd = RunMode.SHARP;

    try
    {
      CommandLineParser parser = new BasicParser();
      CommandLine cmd = parser.parse( o, args );

      if ( cmd.hasOption( "V" ) ) {
        printVersion();
        System.exit( 0 );
      }

      String username = cmd.getOptionValue( USER_NAME );
      if ( username == null || "".equals( username.trim() ) )
      {
        username = System.getProperty( "user.name" );
      }
      if ( username == null || "".equals( username.trim() ) )
      {
        throw new UnrecognizedOptionException( "User name can not be determined, use parameter -" + USER_NAME );
      }

      rnmd = RunMode.getRunMode( cmd.getOptionValue( RUN_MODE ), rnmd );
      ignfail = cmd.hasOption( IGNORE_FAILURES );
      printstacktrace = cmd.hasOption( PRINTSTACKTRACE );
      validateXml = !cmd.hasOption( NO_VALIDATE_XML );
      useLogTable = !cmd.hasOption( NO_LOG_TABLE );
      isIncrimental = cmd.hasOption(INC_MODE);
      String env = cmd.getOptionValue( ENVIRONMENT_OPT );

      String[] a = cmd.getArgs();
      if ( a.length < 1 )
      {
        throw new UnrecognizedOptionException( "Not enough parameters (dbconfig.xml alterscripts...)" );
      }

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

      ApplyAlter applyAlter = new ApplyAlter( a[0], rctx, ignfail, username, validateXml, useLogTable, env );
      applyAlter.applyInternal();
      applyAlter.apply(validateXml, param);
      if ( RunMode.LOOK.equals( rnmd ) )
      {
        rctx.report( MAIN, "Unapplied alters: \n%s", applyAlter.getUnappliedAlters() );
      }
    }
    catch ( UnrecognizedOptionException e )
    {
      System.out.println( e.getMessage() );
      final HelpFormatter helpFormatter = new HelpFormatter();
      helpFormatter.printHelp( "applyalter [options] <dbconfig.xml> (alter.xml|alter.zip) ...", o, false );
      printVersion();
    }
    catch ( Throwable e )
    {
      if ( e instanceof ApplyAlterException && ( !printstacktrace || ignfail ) )
        ( (ApplyAlterException) e ).printMessages( System.err );
      else
        e.printStackTrace( System.err );
      System.exit( -1 );
    }
  }

  private static void printVersion()
  {
    System.out.println();
    try
    {
      final URL jarUrl = ApplyAlter.class.getProtectionDomain().getCodeSource().getLocation();
      System.out.println( jarUrl );
      Manifest man = new JarFile( jarUrl.getFile() ).getManifest();
      final Attributes a = man.getMainAttributes();
      List<String> keys = new ArrayList<String>();
      for ( Object key : a.keySet() )
      {
        final String lk = key.toString().toLowerCase();
        if ( lk.startsWith( "hudson" ) || lk.startsWith( "jenking" ) || lk.startsWith( "git" ) )
          keys.add( key.toString() );
      }
      Collections.sort( keys );
      for ( String key : keys )
      {
        System.out.printf( "%s: %s%n", key, a.getValue( key ) );
      }
    }
    catch (Exception e)
    {
      System.out.printf( "Unknown version (%s)%n", e );
    }
  }

}
