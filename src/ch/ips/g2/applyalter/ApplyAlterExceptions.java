package ch.ips.g2.applyalter;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

/**
 * List of {@link ApplyAlterException}s
 * @author Martin Caslavsky &lt;martin.caslavsky@ips-ag.cz&gt;
 * @version $Id$
 */
public class ApplyAlterExceptions extends ApplyAlterException
{
  protected List<ApplyAlterException> ex = new ArrayList<ApplyAlterException>();
  
  /**
   * Add exception to list
   * @param e exception
   */
  public void add(ApplyAlterException e) {
    if (e instanceof ApplyAlterExceptions)
      this.ex.addAll(((ApplyAlterExceptions)e).ex);
    else
      this.ex.add(e);
  }

  @Override
  public void printMessages(PrintStream out)
  {
    for (ApplyAlterException e: ex)
      e.printMessages(out);
  }
  
  /**
   * Is this list empty?
   * @return true if empty false otherwise
   */
  public boolean isEmpty() {
    return ex.isEmpty();
  }

}
