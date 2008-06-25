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
  index("indexes", "ind", null), 
  /**
   * Routine (procedure, function, method)
   */
  routine,
  
  /**
   * Trigger 
   */
  trigger("tab", "trigname"),
  
  /**
   * foreign key
   */
  reference("tab", "constname"),

  /**
   * check constraint
   */
  check("tab", "constname"),

  /**
   * database column
   */
  column("tab", "colname")
  ;
  
  private String tbl;
  private String abbr;
  private String extra;
  
  private CheckType() {
    this.abbr = this.name();
    setTbl();
  }

  private CheckType(String abbr) {
    this.abbr = abbr;
    setTbl();
  }

  private CheckType(String abbr, String extra) {
    this.abbr = abbr;
    this.extra = extra;
    setTbl();
  }

  private CheckType(String table, String abbr, String extra) {
    this.tbl = table;
    this.abbr = abbr;
    this.extra = extra;
  }

  private void setTbl()
  {
    this.tbl = this.name() + "s";
  }

  public String getTbl()
  {
    return tbl;
  }

  public String getAbbr()
  {
    return abbr;
  }
  
  public String getExtra()
  {
    return extra;
  }

  public String getSQL() {
    String sql = String.format("select * from syscat.%s where %sschema=? and %sname=?", 
        tbl, abbr, abbr);
    if (extra != null)
      sql += String.format(" and %s=?", extra);
    return sql;
  }
  
}
