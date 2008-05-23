package ch.ips.g2.applyalter;

public enum CheckType {
  /**
   * Table 
   */
  table("tab"), 
  /**
   * View
   */
  view, 
  /**
   * Index
   */
  index("ind"), 
  /**
   * Routine (procedure, function, method)
   */
  routine;
  
  private String abbr;
  
  private CheckType() {
    this.abbr = this.name();
  }

  private CheckType(String abbr) {
    this.abbr = abbr;
  }

  public String getAbbr()
  {
    return abbr;
  }
  
  public String getSQL() {
    return String.format("select * from syscat.%ss where %sschema=? and %sname=?", 
        name(), getAbbr(), getAbbr());
  }
  
}
