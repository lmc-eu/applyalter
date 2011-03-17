package ch.ips.g2.applyalter;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

/**
 * List of {@link ApplyAlterException}s
 * @author Martin Caslavsky &lt;martin.caslavsky@ips-ag.cz&gt;
 * @version $Id$
 */
@SuppressWarnings("serial")
public class ApplyAlterExceptions extends ApplyAlterException
{
  protected List<ApplyAlterException> ex = new ArrayList<ApplyAlterException>();
  protected boolean ignorefailures;
  
  /**
   * Should be exceptions collected, or immediately thrown 
   * @param ignorefailures true if collected
   */
  public ApplyAlterExceptions(boolean ignorefailures) {
    this.ignorefailures = ignorefailures;
  }

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
  
  /**
   * Add an exception to the list or throw it, depending on this class constructor parameter
   * @param e exception to collected or to throw
   * @throws ApplyAlterException this method parameter e (if this object is constructed to not collect exceptions) 
   */
  public void addOrThrow(ApplyAlterException e) throws ApplyAlterException {
    if (ignorefailures)
      add(e);
    else
      throw e;
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
