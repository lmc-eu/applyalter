package ch.ips.g2.applyalter;

import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * RunContext used to print to strandad output or to file.
 *
 * @author Kamil Podlesak &lt;kamil.podlesak@ips-ag.cz&gt;
 * @version $Id$
 */
public class PrintWriterRunContext implements RunContext
{
  protected RunMode runMode = RunMode.SHARP;
  protected PrintWriter stdout, stderr;

  /**
   * Datetime formatter; always synchronize when using!
   */
  private final DateFormat dateFormat = new SimpleDateFormat( "'['yyyy-MM-dd HH:mm:ss.SSS'] '" );


  public RunMode getRunMode()
  {
    return runMode;
  }

  public void setRunMode( RunMode runMode )
  {
    this.runMode = runMode;
  }


  /**
   * Create instance with custom print writers.
   *
   * @param stdout the output of nromal messages
   * @param stderr output of errors
   */
  public PrintWriterRunContext( PrintWriter stdout, PrintWriter stderr )
  {
    this.stdout = stdout;
    this.stderr = stderr;
  }

  /**
   * Create instance for standard output and standard error.
   */
  public static PrintWriterRunContext createStdInstance()
  {
    return new PrintWriterRunContext(
        new PrintWriter( System.out, true ),
        new PrintWriter( System.err, true )
    );
  }

  /**
   * Format date and time.
   */
  protected String formatTime( Date time )
  {
    synchronized ( dateFormat )
    {
      return dateFormat.format( time );
    }
  }

  private void writeReport( ReportLevel level, String format, PrintWriter wr, Object... args )
  {
    wr.print( formatTime( new Date() ) );
    wr.printf( format, args );
    wr.println();
  }

  public void report( ReportLevel level, String format, Object... args )
  {
    writeReport( level, format, stdout, args );
  }

  public void report( ReportLevel level, Exception e, String format, Object... args )
  {
    writeReport( level, format, stdout, args );
    writeReport( level, format, stderr, args );
    if ( e != null )
    {
      e.printStackTrace( stderr );
    }
  }

}
