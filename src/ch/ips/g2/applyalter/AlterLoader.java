package ch.ips.g2.applyalter;

import ch.ips.base.BaseUtil;
import org.xml.sax.SAXException;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Validator;
import java.io.InputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Enumeration;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Collections;
import java.util.TreeMap;
import java.util.zip.ZipFile;
import java.util.zip.ZipEntry;
import com.thoughtworks.xstream.XStreamException;
import com.thoughtworks.xstream.XStream;

public class AlterLoader
{
  /**
   * XStream deserializer, correctly initialized.
   */
  protected final XStream xstream;
  /**
   * XML validator
   */
  protected final Validator validator;

  public AlterLoader( XStream xstream, Validator validator )
  {
    this.xstream = xstream;
    this.validator = validator;
  }

  /**
   * Create new instance from XML serialized form
   *
   * @param file identifier for {@link ch.ips.g2.applyalter.Alter#setId(String)} ; can be fake file path
   * @param i input stream to read from
   * @return new instance
   */
  public Alter newAlter( String file, InputStream i )
  {
    Alter a = null;
    try
    {
      a = (Alter) xstream.fromXML( i );
    }
    catch (XStreamException e)
    {
      throw new ApplyAlterException( "Unable to deserialize Alter from file " + file, e );
    }
    // get file name part
    a.setId( new File( file ).getName() );
    return a;
  }

  /**
   * Load alterscripts from files and ZIP archives.
   */
  public List<Alter> loadAlters( String... filenames )
  {
    List<Alter> a = new ArrayList<Alter>( filenames.length );
    for ( String f : filenames )
    {
      if ( f.endsWith( ApplyAlter.XML_SUFFIX ) )
      {
        a.add( fromFile( f ) );
      }
      else if ( f.endsWith( ApplyAlter.ZIP_SUFFIX ) )
      {
        a.addAll( fromZip( f ) );
      }
      else
      {
        throw new ApplyAlterException( "Unknown filetype " + f );
      }
    }
    return a;
  }

  /**
   * Parse script file, validate it and load datafiles (if any).
   */
  private Alter parseScriptFile( String fileId, AlterSource source )
  {
    Alter alterscript;
    InputStream inputStream = null;
    try
    {
      if ( validator != null )
      {
        validator.validate( new StreamSource( source.openScript() ) );
      }
      inputStream = source.openScript();
      alterscript = newAlter( fileId, inputStream );

    }
    catch (FileNotFoundException e)
    {
      throw new ApplyAlterException( "File not found " + fileId, e );
    }
    catch (SAXException e)
    {
      throw new ApplyAlterException( "Can not validate file " + fileId, e );
    }
    catch (IOException e)
    {
      throw new ApplyAlterException( "I/O exception during XML file validation " + fileId, e );
    }
    finally
    {
      BaseUtil.closeNoThrow( inputStream, "parseScriptFile" );
    }

    //load
    if ( alterscript.datafile != null && alterscript.datafile.size() > 0 )
    {
      alterscript._datafiles = new TreeMap<String, byte[]>();
      for ( String datafile : alterscript.datafile )
      {
        inputStream = null;
        try
        {
          inputStream = source.openDataFile( datafile );
          byte[] data = BaseUtil.getBytesFormInputStream( inputStream );
          alterscript._datafiles.put( datafile, data );
        }
        catch (FileNotFoundException e)
        {
          throw new ApplyAlterException( "no such datafile: " + datafile + " for " + fileId, e );
        }
        catch (IOException e)
        {
          throw new ApplyAlterException( "error reading datafile: " + datafile + " for " + fileId, e );
        }
        finally
        {
          BaseUtil.closeNoThrow( inputStream, "parseScriptFile" );
        }
      }
    }

    return alterscript;
  }

  /**
   * Create Alter instance from XML serialized from file
   *
   * @param file XML serialized Alter
   * @return new Alter instance
   * @throws ch.ips.g2.applyalter.ApplyAlterException if file can not be found
   */
  protected Alter fromFile( String file )
      throws ApplyAlterException
  {
    AlterSource source = new FileSource( new File( file ) );
    return parseScriptFile( file, source );
  }

  /**
   * Create a list of Alter instances from XML serialized from files stored in .zip.
   * List is sorted using {@link ch.ips.g2.applyalter.ZipEntryNameComparator}.
   *
   * @param zipfile zip file containing XML files
   * @return list of new Alter instances
   * @throws ch.ips.g2.applyalter.ApplyAlterException if error occurs during zip file processing
   */
  protected List<Alter> fromZip( String zipfile )
  {
    try
    {
      final ZipFile z = new ZipFile( zipfile );
      List<ZipEntry> alterEntries = new ArrayList<ZipEntry>();
      final Map<String, ZipEntry> allFiles = new HashMap<String, ZipEntry>();

      Enumeration<? extends ZipEntry> zipEntryEnum = z.entries();
      while ( zipEntryEnum.hasMoreElements() )
      {
        ZipEntry zipFile = zipEntryEnum.nextElement();
        if ( zipFile.isDirectory() )
        {
          continue;
        }
        allFiles.put( zipFile.getName(), zipFile );
        if ( zipFile.getName().endsWith( ApplyAlter.XML_SUFFIX ) )
        {
          alterEntries.add( zipFile );
        }
      }

      List<Alter> a = new ArrayList<Alter>( alterEntries.size() );
      Collections.sort( alterEntries, new ZipEntryNameComparator() );
      for ( final ZipEntry entry : alterEntries )
      {
        AlterSource source = new ZipAlterSource( z, entry, allFiles );

        String file = z.getName() + "!" + entry.getName();
        Alter alterscript = parseScriptFile( file, source );
        a.add( alterscript );
      }

      return a;
    }
    catch (IOException e)
    {
      throw new ApplyAlterException( "Error reading zip file " + zipfile, e );
    }
  }

  //-----------------------------------------------------------------------------------------------------------------

  protected interface AlterSource
  {
    InputStream openScript()
        throws IOException;

    /**
     * Open data file (from the same directory/archive).
     *
     * @param filename filename (relative)
     * @return data to read
     * @throws FileNotFoundException no such file
     * @throws IOException error opening
     */
    InputStream openDataFile( String filename )
        throws IOException;
  }

  private static class FileSource implements AlterSource
  {
    private File file;

    public FileSource( File file )
    {
      this.file = file;
    }

    public InputStream openScript()
        throws IOException
    {
      return new FileInputStream( file );
    }

    public InputStream openDataFile( String filename )
        throws IOException
    {
      File dataFile = new File( file.getParentFile(), filename );
      return new FileInputStream( dataFile );
    }
  }

  private static class ZipAlterSource implements AlterSource
  {
    private final ZipFile zip;
    private final ZipEntry scriptEntry;
    private final Map<String, ZipEntry> allFiles;

    public ZipAlterSource( ZipFile zip, ZipEntry scriptEntry, Map<String, ZipEntry> allFiles )
    {
      this.zip = zip;
      this.scriptEntry = scriptEntry;
      this.allFiles = allFiles;
    }

    public InputStream openScript()
        throws IOException
    {
      return zip.getInputStream( scriptEntry );
    }

    public InputStream openDataFile( String filename )
        throws IOException
    {
      ZipEntry dataEntry = allFiles.get( filename );
      if ( dataEntry == null )
        throw new FileNotFoundException( "no such file in archive: " + filename + " in " + zip.getName() );
      return zip.getInputStream( dataEntry );
    }
  }
}