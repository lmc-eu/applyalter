package ch.ips.g2.applyalter;

import java.sql.Connection;

/**
 * Transaction isolation level. Names are taken from DB2.
 *
 * @author Kamil Podlesak &lt;kamil.podlesak@ips-ag.cz&gt;
 * @version $Id$
 */
public enum IsolationLevel
{
  /**
   * Uncommited Read.
   */
  UR( Connection.TRANSACTION_READ_UNCOMMITTED ),
  /**
   * Cursor Stability.
   */
  CS( Connection.TRANSACTION_READ_COMMITTED ),
  /**
   * Read Stability.
   */
  RS( Connection.TRANSACTION_REPEATABLE_READ ),
  /**
   * Repeatable Read  = the highest isolation level.
   */
  RR( Connection.TRANSACTION_SERIALIZABLE );

  private final int jdbcValue;

  /**
   * Value used in JDBC.
   */
  public int getJdbcValue()
  {
    return jdbcValue;
  }

  IsolationLevel( int jdbcValue )
  {
    this.jdbcValue = jdbcValue;
  }
}
