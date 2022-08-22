package ch.ips.g2.applyalter;

import com.thoughtworks.xstream.XStream;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class DbConfigFileTest {

    @Test
    public void generateConfigXmlTest() {
        DbConfigFile cfg = new DbConfigFile();
        cfg.environment = "dev26";
        DbInstance instance = new PgInstance("dbtest", "master", "dbtest.dev26.lmc", 5432, "test", "testUser", "secret");
        cfg.instances.add(instance);

        cfg.placeholders = new ArrayList<>();
        cfg.placeholders.add(new DbCustomParam("roleRW", "pgs_brand0_rw"));
        cfg.placeholders.add(new DbCustomParam("roleRO", "pgs_brand0_ro"));

        XStream xstream = new XStream();
        xstream.processAnnotations(new Class[]{
                DbConfigFile.class,
                PgInstance.class,
                DbCustomParam.class
        });
        String xml = xstream.toXML(cfg);

        System.out.println(xml);

        assertTrue(xml.contains("<param name=\"roleRW\" value=\"pgs_brand0_rw\"/>"), "Final xml must contain element param with attribute name= roleRW");
    }
}
