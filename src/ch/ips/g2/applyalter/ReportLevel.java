package ch.ips.g2.applyalter;

/**
 * Report level of the tool output: ranges from important messages and errors to debugging notes.
 *
 * @author Kamil Podlesak &lt;kamil.podlesak@ips-ag.cz&gt;
 * @version $Id$
 */
public enum ReportLevel
{
  /**
   * Fatal error, the tools has to quit (probably).
   */
  FATAL,
  /**
   * Any general error.
   */
  ERROR,

  /**
   * Main messages of processing: start, stop, currently processed alterscript.
   */
  MAIN,

  /**
   * Messages about currently processed alterscript: names of processed statements.
   */
  ALTER,

  /**
   * Messages about currently processed statements: full statement info.
   */
  STATEMENT,

  /**
   * Statement steps: the executed sql commands.
   */
  STATEMENT_STEP,

  /**
   * Details (useful for debugging).
   */
  DETAIL,


}
