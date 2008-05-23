package ch.ips.g2.applyalter;

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
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;


/**
 * Apply alter scripts to database instances
 * @author Martin Caslavsky &lt;martin.caslavsky@ips-ag.cz&gt;
 * @version $Id$
 */
public class ApplyAlter
{
  public static final String CHECK_OK = "OK";
  /**
   * Run mode system property name
   */
  public static final String RUN_MODE = "runmode";
  /**
   * Ignore failures system property name
   */
  public static final String IGNORE_FAILURES = "ignorefailures";
  /**
   * Suffix for zip file
   */
  public static final String ZIP_SUFFIX = ".zip";
  /**
   * Suffix for xml file with serialized Alter
   */
  public static final String XML_SUFFIX = ".xml";
  /**
   * Configuration of database instances
   */
  protected DbConfig db;
  /**
   * run mode
   */
  protected RunMode runmode;
  
  /**
   * Create instance
   * @param dbconfigfile XML serialized {@link DbConfig} (database configuration)
   */
  public ApplyAlter(String dbconfigfile, RunMode runmode, boolean ignorefailures) {
    db = new DbConfig(dbconfigfile, ignorefailures);
    this.runmode = runmode;
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
   * Create Alter instance from XML serialized from file 
   * @param file XML serialized Alter
   * @return new Alter instance
   * @throws ApplyAlterException if file can not be found
   */
  protected static Alter fromFile(String file) throws ApplyAlterException
  {
    try {
      FileInputStream i = new FileInputStream(file);
      return Alter.newInstance( file, i);
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
  protected static List<Alter> fromZip(String zipfile) {
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
        a.add(Alter.newInstance(id, s));
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
  protected static boolean check(Connection c, Check a) throws ApplyAlterException
  {
    a.check();
    PreparedStatement s = null;
    try {
      String sql = a.getType().getSQL();
      String schema = a.getSchema().toUpperCase();
      String name = a.getName().toUpperCase();
      System.out.printf("Check: %s %s %s\n", sql, schema, name);
      s = c.prepareStatement(sql);
      s.setString(1, schema);
      s.setString(2, name);
      s.execute();
      return s.getResultSet().next();
    } catch (SQLException e) {
      throw new ApplyAlterException("Can not check " + a, e);
    } finally {
      if (s != null)
        try {
          s.close();
        } catch (SQLException e) {}
    }
  }

  /**
   * Custom check if an alter was applieds already. 
   * @param c Connection to database
   * @param sql custom SQL statement
   * @return true if sql is not null and result of sql statement is equal {@link #CHECK_OK} value
   * @throws ApplyAlterException
   */
  protected static boolean check(Connection c, String sql) throws ApplyAlterException
  {
    if (sql == null)
      return false;
    PreparedStatement s = null;
    try {
      System.out.printf("Check: %s\n", sql);
      s = c.prepareStatement(sql);
      ResultSet rs = s.executeQuery();
      rs.next();
      String check = rs.getString(1);
      return (CHECK_OK.equalsIgnoreCase(check));
    } catch (SQLException e) {
      throw new ApplyAlterException("Can not check " + sql, e);
    } finally {
      if (s != null)
        try {
          s.close();
        } catch (SQLException e) {}
    }
  }
/*
 */
  
  /**
   * Apply alter scripts to all or selected database instances
   * @param alters alter scripts to apply
   * @throws ApplyAlterException if one of statements can not be executed
   */
  public void apply(Alter... alters) throws ApplyAlterException {
    String dbid = null;
    String statement = null;
    PreparedStatement t = null;
    ApplyAlterExceptions aae = new ApplyAlterExceptions();
    try {
      // for all alter scripts
      for (Alter a: alters) {
        // for all (or selected) databases
        DbLoop:
        for (DbInstance d: db.getEntries()) {
          // apply to this instance?
          if (a.isAllInstances() || a.getInstances().contains(d.getType())) {
            long start = System.currentTimeMillis();
            dbid = d.getId();
            try {
              Connection c = d.getConnection();
              System.out.printf("Database instance %s %s\n", dbid, d.getUrl());
  
              // do checks
              if (check(c, a.getCheck())) {
                System.out.println("Alter applied already, skipping");
                continue DbLoop;
              }
              for (Check i: a.getChecks())
                if (check(c, i)) {
                  System.out.println("Alter applied already, skipping");
                  continue DbLoop;
                }
              
              d.useConnection();
              // for all alter statements
              for (AlterStatement s: a.getStatements()) {
                System.out.println(s);
                String stm = s.getSQLStatement();
                t = null;
                statement = s.toString();
                if (stm == null || RunMode.print.equals(runmode))
                  continue;
                t = s.getPreparedStatement(c);
                t.execute();
              }
              long stop = System.currentTimeMillis();
              System.out.printf("Alter %s on %s took %s ms\n", a.getId(), dbid, stop-start);
              
            } catch (SQLException e) {
              ApplyAlterException ex = new ApplyAlterException("Can not execute alter statement on db " + dbid + "\n" + statement, e);
              if (db.isIgnorefailures()) aae.add(ex);
              else throw ex;
            } catch (ApplyAlterException e) {
              if (db.isIgnorefailures()) aae.add(e);
              else throw e;
            } finally {
              if (t != null)
                try {
                  t.close();
                } catch (SQLException e) {}
            }
          }
        }
        // commit each alter on used databases
        if (aae.isEmpty() && RunMode.sharp.equals(runmode))
          db.commitUsed();
      }
    
    } finally {
      db.closeConnections();
    }
    if (!aae.isEmpty()) throw aae;
  }

    
  public static void main(String[] args)
  {
    boolean ignfail = Boolean.getBoolean(IGNORE_FAILURES);
    boolean printstacktrace = Boolean.getBoolean("printstacktrace");
    RunMode rnmd = RunMode.getProperty(RUN_MODE, RunMode.sharp);

    try {
      if (args.length < 1)
        throw new ApplyAlterException("Run with params <dbconfig.xml> (alter.xml|alter.zip) ...");
      
      // prepare arguments
      String[] param = new String[args.length-1];
      System.arraycopy(args, 1, param, 0, args.length-1);
      
      // go
      System.out.printf("ApplyAlter started in run mode: %s\n", rnmd);
      new ApplyAlter(args[0], rnmd, ignfail)
        .apply(param);
      
    } catch (ApplyAlterException e) {
      if (printstacktrace && !ignfail)
        e.printStackTrace(System.err);
      else
        e.printMessages(System.err);
      System.exit(-1);
    }
  }

}
