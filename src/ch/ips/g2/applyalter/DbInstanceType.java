package ch.ips.g2.applyalter;

import com.thoughtworks.xstream.annotations.XStreamAlias;

@XStreamAlias("instance")
public enum DbInstanceType {
  master, slave
}
