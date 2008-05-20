package ch.ips.g2.applyalter;

/**
 * Apply Alter run mode (see values).
 * @author Martin Caslavsky &lt;martin.caslavsky@ips-ag.cz&gt;
 * @version $Id$
 */
public enum RunMode {
  /**
   * Execute statements and commit them
   */
  sharp, 
  /**
   * Execute statements but dont commit them
   */
  dry, 
  /**
   * Just print statements
   */
  print;
  
  /**
   * Get new instance of RunMode from system property
   * @param name system property name
   * @param def default value
   * @return RunMode from system property or default value
   */
  public static RunMode getProperty(String name, RunMode def) {
    RunMode result = def;
    String p = System.getProperty(name);
    try {
      if (p != null)
        result = RunMode.valueOf(p);
    } catch (RuntimeException e) {
      // return default
    }
    return result;
  }
  
  
}
