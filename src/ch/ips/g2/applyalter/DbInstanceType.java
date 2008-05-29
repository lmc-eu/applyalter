package ch.ips.g2.applyalter;

import com.thoughtworks.xstream.annotations.XStreamAlias;

/**
 * Type of database instance (master/slave)
 * @author Martin Caslavsky &lt;martin.caslavsky@ips-ag.cz&gt;
 * @version $Id$
 */
@XStreamAlias("instance")
public enum DbInstanceType {
  master, slave
}
