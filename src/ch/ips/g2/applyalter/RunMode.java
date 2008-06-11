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
  SHARP,
  /**
   * Execute statements but dont commit them
   */
  DRY,
  /**
   * Just print statements
   */
  PRINT;
  
  /**
   * Get new instance of RunMode from system property
   * @param name system property name
   * @param def default value
   * @return RunMode from system property or default value
   */
  public static RunMode getProperty(String name, RunMode def) {
    String p = System.getProperty(name);
    return getRunMode(p, def);
  }

  /**
   * Get new Instance of RunMode from string
   * @param p string
   * @param def default value
   * @return RunMode from string or default value
   */
  public static RunMode getRunMode(String p, RunMode def)
  {
    RunMode result = def;
    try {
      if (p != null)
        result = RunMode.valueOf(p);
    } catch (RuntimeException e) {
      // return default
    }
    return result;
  }
  
  
}
