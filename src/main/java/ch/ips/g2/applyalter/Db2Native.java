package ch.ips.g2.applyalter;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import org.apache.commons.io.IOUtils;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * DB2 JDBC app driver: requires native driver, but can skip the username and password (using the system
 * user authorization).
 *
 * @author Kamil Podlesak &lt;kamil.podlesak@ips-ag.cz&gt;
 * @version $Id$
 */
@XStreamAlias("db2native")
public class Db2Native extends Db2Instance {
    //private static final String DB_DRIVER = "COM.ibm.db2.jdbc.app.DB2Driver";

    public String driver;

    public Db2Native() {
    }

    public Db2Native(String id, String type, String host, Integer port, String db, String user, String pass) {
        super(id, type, host, port, db, user, pass);
    }


    /**
     * Get url for connecting <code>jdbc:db2://...</code>
     *
     * @return url for connectiong
     */
    @Override
    public String getUrl() {
        return String.format("jdbc:db2:%s", db);
    }

    /**
     * Connect the database: if {@link #user} is null or empty, the passwordless variant of
     * {@link DriverManager#getConnection(String)} is called. DB2 will then use system user.
     */
    @Override
    protected Connection connect(String url, RunContext ctx)
            throws SQLException {
        try {
            if (driver != null) {
                Class.forName(driver);
            } else {
                Class.forName(Db2Instance.DB_DRIVER);
            }
        } catch (ClassNotFoundException e) {
            throw new ApplyAlterException("Can not initialize db driver " + driver, e);
        }

        //note: user checking is done in superclass implementation
        return super.connect(url, ctx);
    }

    /**
     * There is no host in this configuration, so default implementaion is useless. It is, however, to use
     * /etc/lmcenv.xml
     *
     * @param runContext run contet (used for logging)
     * @return environment form /etc/lmcenv.xml <br />null if it cannot be parsed
     */
    @Override
    public String guessEnvironment(RunContext runContext) {
        final String cfgPath = "/etc/lmcenv.xml";
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(cfgPath);
            DocumentBuilderFactory docBuildFact = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docBuildFact.newDocumentBuilder();
            final Element root = docBuilder.parse(fis).getDocumentElement();
            final String envName = root.getAttribute("name").trim();
            if (envName.length() > 0) {
                runContext.report(ReportLevel.DETAIL, "environment read from %s: %s", cfgPath, envName);
                return envName;
            }
        } catch (IOException e) {
            runContext.report(ReportLevel.DETAIL, "failed to read %s: %s", cfgPath, e);
        } catch (Exception e) {
            runContext.report(ReportLevel.DETAIL, e, "failed to parse %s", cfgPath);
        } finally {
            IOUtils.closeQuietly(fis);
        }
        return null;
    }

}