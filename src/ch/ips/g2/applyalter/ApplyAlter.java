package ch.ips.g2.applyalter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;


/**
 * Apply alter scripts to database instances
 * @author Martin Caslavsky &lt;martin.caslavsky@ips-ag.cz&gt;
 * @version $Id$
 */
public class ApplyAlter
{

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
  protected boolean ignorefailures;
  
  /**
   * Create instance
   * @param dbconfigfile XML serialized {@link DbConfig} (database configuration)
   */
  public ApplyAlter(String dbconfigfile, boolean ignorefailures) {
    db = new DbConfig(dbconfigfile);
    this.ignorefailures = ignorefailures;
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
   * Create a list of Alter instances from XML serialized from files stored in .zip 
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
   * Check if all database identifiers in alters are defined in database configuration
   * @param alters to check
   * @throws ApplyAlterException if there is an unknown database id
   */
  protected void checkDbIds(Alter... alters) throws ApplyAlterException
  {
    for (Alter a: alters)
      for (String i: a.getInstances())
        if (!db.getDbIds().contains(i))
          throw new ApplyAlterException("Unknown database id " + i + " in alter " + a.getId());
  }

  /**
   * Apply alter scripts to all or selected database instances
   * @param alters alter scripts to apply
   * @throws ApplyAlterException if one of statements can not be executed
   */
  public void apply(Alter... alters) throws ApplyAlterException {
    String dbid = null;
    String statement = null;
    Statement t = null;
    ApplyAlterExceptions aae = new ApplyAlterExceptions();
    try {
      checkDbIds(alters);
      // for all alter scripts
      for (Alter a: alters) {
        // for all (or selected) databases
        for (Map.Entry<String, DbInstance> i: db.getEntries()) {
          // apply to this instance?
          if (a.isAllInstances() || a.getInstances().contains(i.getKey())) {
            long start = System.currentTimeMillis();
            dbid = i.getKey();
            try {
              DbInstance d = i.getValue();
              Connection c = d.useConnection();
              System.out.printf("Database instance %s %s\n", dbid, d.getUrl());
              // for all alter statements
              for (AlterStatement s: a.getStatements()) {
                System.out.println(s);
                String stm = s.getSQLStatement();
                t = null;
                statement = stm;
                if (stm == null)
                  continue;
                t = c.createStatement();
                t.execute(stm);
              }
              long stop = System.currentTimeMillis();
              System.out.printf("Alter %s on %s took %s ms\n", a.getId(), dbid, stop-start);
              
              // do check
              statement = a.getCheck(); 
              if (statement != null) {
                t = c.createStatement();
                ResultSet rs = t.executeQuery(statement);
                String check = rs.getString(0);
                if (!"OK".equalsIgnoreCase(check))
                  throw new ApplyAlterException("Check on db " + dbid + " failed: " + check);
              }

            } catch (SQLException e) {
              ApplyAlterException ex = new ApplyAlterException("Can not execute alter statement on db " + dbid + "\n" + statement, e);
              if (ignorefailures) aae.add(ex);
              else throw ex;
            } catch (ApplyAlterException e) {
              if (ignorefailures) aae.add(e);
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
        db.commitUsed(ignorefailures);
      }
    
    } finally {
      db.closeConnections();
    }
    if (!aae.isEmpty()) throw aae;
  }
  
  public static void main(String[] args)
  {
    try {
      if (args.length < 1)
        throw new ApplyAlterException("Run with params <dbconfig.xml> (alter.xml|alter.zip) ...");
      String[] param = new String[args.length-1];
      System.arraycopy(args, 1, param, 0, args.length-1);
      new ApplyAlter(args[0], Boolean.getBoolean(IGNORE_FAILURES)).apply(param);
    } catch (ApplyAlterException e) {
      e.printMessages(System.err);
      System.exit(-1);
    }
  }

}
