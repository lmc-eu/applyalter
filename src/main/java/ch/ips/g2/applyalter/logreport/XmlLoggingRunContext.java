package ch.ips.g2.applyalter.logreport;

import ch.ips.g2.applyalter.ReportLevel;
import ch.ips.g2.applyalter.RunContext;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.lang.time.DateFormatUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.annotation.Nonnull;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.IOException;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

/**
 * Collect logs into json.
 */
public class XmlLoggingRunContext extends RunContext.WrapperRunContext {

    protected final Document document;
    private final Transformer xmlTransformer;
    private OutputStream fos;

    @Nonnull
    private final Element root;
    @Nonnull
    private Element current;

    /**
     * Format date and time.
     */
    protected String formatTime(Date time) {
        return DateFormatUtils.ISO_DATETIME_TIME_ZONE_FORMAT.format(time);
    }


    public XmlLoggingRunContext(RunContext wrapped, OutputStream fos) {
        super(wrapped);
        this.fos = fos;

        try {
            DocumentBuilderFactory xmlf = DocumentBuilderFactory.newInstance();
            this.document = xmlf.newDocumentBuilder().newDocument();
        } catch (ParserConfigurationException e) {
            throw new IllegalStateException("bug: non-functioning xml???", e);
        }

        try {
            TransformerFactory tf = TransformerFactory.newInstance();
            this.xmlTransformer = tf.newTransformer();
            xmlTransformer.setOutputProperty(OutputKeys.INDENT, "yes");
            xmlTransformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            xmlTransformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        } catch (TransformerConfigurationException e) {
            throw new IllegalStateException("bug: non-functioning xml???", e);
        }

        //and start collecting
        this.current = this.root = document.createElement("log");
        document.appendChild(current);
    }

    @Override
    public void close() throws IOException {
        super.close();

        if (this.fos != null) {

            try {
                //write
                xmlTransformer.transform(new DOMSource(root), new StreamResult(fos));
            } catch (TransformerException e) {
                //where to report log error?
                e.printStackTrace();
            }

            fos.close();
            this.fos = null;
        }
        //reset hierarchy, just to be sure
        this.current = this.root;
    }

    @Override
    public void subreport(String subreportName, Runnable run) {
        final Element backup = this.current;
        final long start = System.currentTimeMillis();
        try {
            //add new structured record...
            this.current = document.createElement(subreportName);
            backup.appendChild(this.current);
            super.subreport(subreportName, run);
        } finally {
            //log time
            final long time = System.currentTimeMillis() - start;
            this.current.setAttribute("duration", String.valueOf(time));
            //and pop the stack
            this.current = backup;
        }
    }

    @Override
    public void reportProperty(ReportLevel level, String key, Object value) {
        this.current.setAttribute(key, String.valueOf(value));
        //also log to standard output
        super.reportProperty(level, key, value);
    }

    @Override
    public void report(ReportLevel level, String format, Object... args) {
        super.report(level, format, args);
        createMessageElement(format, args);
    }

    private Element createMessageElement(String format, Object[] args) {
        Element msg = document.createElement("message");
        msg.setTextContent(String.format(format, args).trim());
        msg.setAttribute("at", formatTime(new Date()));
        current.appendChild(msg);
        return msg;
    }

    @Override
    public void report(ReportLevel level, Exception e, String format, Object... args) {
        super.report(level, e, format, args);
        final Element msg = createMessageElement(format, args);
        if (e != null) {
            Element stacktrace = document.createElement("stacktrace");
            stacktrace.setTextContent(ExceptionUtils.getFullStackTrace(e));
            msg.appendChild(stacktrace);
        }
    }

}
