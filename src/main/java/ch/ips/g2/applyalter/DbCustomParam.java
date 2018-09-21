package ch.ips.g2.applyalter;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;

@XStreamAlias("param")
public class DbCustomParam {
    @XStreamAsAttribute
    protected String name;

    @XStreamAsAttribute
    protected String value;

    public DbCustomParam() {
    }

    public DbCustomParam(String name, String value) {
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }
}
