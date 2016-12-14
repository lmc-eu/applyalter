package ch.ips.g2.applyalter.logreport;

import ch.ips.g2.applyalter.ReportLevel;
import ch.ips.g2.applyalter.RunContext;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonWriter;
import org.apache.commons.lang.exception.ExceptionUtils;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;

/**
 * Collect logs into json.
 */
public class JsonLoggingRunContext extends RunContext.WrapperRunContext {

    protected final Gson gson;
    private JsonWriter jsonWriter;

    @Nonnull
    private final LoggingLevel root;
    @Nonnull
    private LoggingLevel current;

    protected static class LoggingLevel {
        final JsonObject structured;
        final JsonArray records;

        public LoggingLevel() {
            this.structured = new JsonObject();
            this.records = new JsonArray();
            this.structured.add("records", this.records);
        }
    }

    public JsonLoggingRunContext(RunContext wrapped, OutputStream fos) {
        super(wrapped);
        this.gson = new Gson();
        try {
            this.jsonWriter = new JsonWriter(new OutputStreamWriter(fos, "UTF-8"));
            this.jsonWriter.setIndent("  ");
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException("UTF-8 is not supported?", e);
        }

        //and start collecting
        this.current = this.root = new LoggingLevel();
    }

    @Override
    public void close() throws IOException {
        super.close();

        if (this.jsonWriter != null) {

            //write
            gson.toJson(root.structured, jsonWriter);

            jsonWriter.close();
            this.jsonWriter = null;
        }
        //reset hierarchy, just to be sure
        this.current = this.root;
    }

    @Override
    public void subreport(Runnable run) {
        final LoggingLevel backup = this.current;
        final long start = System.currentTimeMillis();
        try {
            //add new structured record...
            this.current = new LoggingLevel();
            backup.records.add(this.current.structured);
            super.subreport(run);
        } finally {
            //log time
            final long time = System.currentTimeMillis() - start;
            this.current.structured.addProperty("duration", time);
            //and pop the stack
            this.current = backup;
        }
    }

    @Override
    public void reportStructuredProperty(String key, Object value) {
        this.current.structured.add(key, gson.toJsonTree(value));
        super.reportStructuredProperty(key, value);
    }

    @Override
    public void report(ReportLevel level, String format, Object... args) {
        super.report(level, format, args);
        current.records.add(String.format(format, args));
    }

    @Override
    public void report(ReportLevel level, Exception e, String format, Object... args) {
        super.report(level, e, format, args);
        final JsonObject o = new JsonObject();
        if (e != null) {
            o.addProperty("stacktrace", ExceptionUtils.getFullStackTrace(e));
        }
        o.addProperty("message", String.format(format, args));
        current.records.add(o);
    }

}
