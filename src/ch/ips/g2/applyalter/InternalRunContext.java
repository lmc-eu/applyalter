package ch.ips.g2.applyalter;

/**
 * Internal run context: used to run internal alterscript. Nothing should be printed to user.
 *
 * @author Kamil Podlesak &lt;kamil.podlesak@ips-ag.cz&gt;
 * @version $Id$
 */
class InternalRunContext implements RunContext
{
  public RunMode getRunMode()
  {
    return RunMode.SHARP;
  }

  public void report( ReportLevel level, String format, Object... args )
  {
    //skip
  }

  public void report( ReportLevel level, Exception e, String format, Object... args )
  {
    //skip
  }
}
