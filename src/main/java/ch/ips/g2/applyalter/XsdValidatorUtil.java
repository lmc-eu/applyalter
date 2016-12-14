package ch.ips.g2.applyalter;

import org.xml.sax.SAXException;

import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.File;

import static ch.ips.g2.applyalter.ReportLevel.MAIN;

/**
 * Load and compile XSD validator. This code used to be in {@link ApplyAlter}, but that class is already cluttered.
 *
 * @author Kamil Podlesak &lt;kamil.podlesak@lmc.eu&gt;
 */
class XsdValidatorUtil {

    /**
     * Read XSD file applyalter.xsd and construct validator.
     *
     * @param runContext execution context, providing methods to output the results and report the processing steps.
     * @throws ApplyAlterException error parsing xsd
     */
    static Validator readXsd(RunContext runContext)
            throws ApplyAlterException {
        try {
            // 1. Lookup a factory for the W3C XML Schema language
            SchemaFactory factory = SchemaFactory.newInstance("http://www.w3.org/2001/XMLSchema");

            // 2. Compile the schema.
            Schema schema = compileXMLSchema(runContext, factory);

            // 3. Get a validator from the schema and return it.
            return schema.newValidator();
        } catch (SAXException e) {
            throw new ApplyAlterException("Unable to initialize XML validator", e);
        }
    }

    /**
     * Read XSD file and compile XML schema.
     *
     * @param runContext execution context, providing methods to output the results and report the processing steps.
     * @param factory    factory that creates Schema objects. Entry-point to the validation API.
     * @return Immutable in-memory representation of XML grammar.
     * @throws SAXException error parsing xsd
     */
    private static Schema compileXMLSchema(RunContext runContext, SchemaFactory factory)
            throws SAXException {
        Schema schema = null;

        try {
            File schemaLocation = new File("applyalter.xsd");
            schema = factory.newSchema(schemaLocation);
            if (schema != null) {
                runContext.report(MAIN, "applyalter.xsd successfuly read from file");
            }
        } catch (Exception e) {
            //ignore exception
            runContext.report(MAIN, "Cannot read applyalter.xsd from file, trying read it from applyalter.jar");
        }
        if (schema == null) {
            StreamSource inputSource = new StreamSource(ApplyAlter.class.getResourceAsStream("/applyalter.xsd"));
            schema = factory.newSchema(inputSource);
            if (schema != null) {
                runContext.report(MAIN, "applyalter.xsd read successfuly read from applyalter.jar");
            }
        }
        return schema;
    }


}
