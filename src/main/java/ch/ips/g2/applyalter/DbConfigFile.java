package ch.ips.g2.applyalter;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamImplicit;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Kamil Podlesak &lt;kamil.podlesak@lmc.eu&gt;
 */
@XStreamAlias("db")
public class DbConfigFile {
    protected String environment;
    @XStreamImplicit()
    protected final List<DbInstance> instances = new ArrayList<>();

    protected List<DbCustomParam> placeholders;
}
