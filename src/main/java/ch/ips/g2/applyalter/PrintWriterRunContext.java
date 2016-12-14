package ch.ips.g2.applyalter;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * RunContext used to print to strandad output or to file.
 *
 * @author Kamil Podlesak &lt;kamil.podlesak@ips-ag.cz&gt;
 * @version $Id$
 */
public class PrintWriterRunContext implements RunContext {
    protected RunMode runMode = RunMode.SHARP;
    private boolean incremental;
    @Nullable
    private ReportLevel quietLevel;
    protected PrintWriter stdout, stderr;

    /**
     * Datetime formatter; always synchronize when using!
     */
    private final DateFormat dateFormat = new SimpleDateFormat("'['yyyy-MM-dd HH:mm:ss.SSS'] '");


    public RunMode getRunMode() {
        return runMode;
    }

    public void setRunMode(RunMode runMode) {
        this.runMode = runMode;
    }

    public boolean isIncremental() {
        return incremental;
    }

    public void setIncremental(boolean incremental) {
        this.incremental = incremental;
    }

    @Nullable
    public ReportLevel getQuietLevel() {
        return quietLevel;
    }

    public void setQuietLevel(@Nullable ReportLevel quietLevel) {
        this.quietLevel = quietLevel;
    }

    /**
     * Create instance with custom print writers.
     *
     * @param stdout the output of nromal messages
     * @param stderr output of errors
     */
    public PrintWriterRunContext(PrintWriter stdout, PrintWriter stderr) {
        this.stdout = stdout;
        this.stderr = stderr;
    }

    public void close() throws IOException {
        //flush, but don't close!
        stdout.flush();
        stderr.flush();
    }

    /**
     * Create instance for standard output and standard error.
     */
    public static PrintWriterRunContext createStdInstance() {
        return new PrintWriterRunContext(
                new PrintWriter(System.out, true),
                new PrintWriter(System.err, true)
        );
    }

    public static PrintWriterRunContext createInstance(boolean isIncrimental, RunMode rnmd,
                                                       @Nullable ReportLevel quietLevel) {
        PrintWriterRunContext rctx = createStdInstance();
        rctx.setRunMode(rnmd);
        rctx.setIncremental(isIncrimental);
        rctx.setQuietLevel(quietLevel);
        return rctx;
    }

    /**
     * Format date and time.
     */
    protected String formatTime(Date time) {
        synchronized (dateFormat) {
            return dateFormat.format(time);
        }
    }

    private boolean isQuiet(ReportLevel level) {
        return getQuietLevel() != null && getQuietLevel().compareTo(level) <= 0;
    }

    private void writeReport(ReportLevel level, String format, PrintWriter wr, Object... args) {
        if (isQuiet(level)) {
            return;
        }
        wr.print(formatTime(new Date()));
        wr.printf(format, args);
        wr.println();
    }

    public void report(ReportLevel level, String format, Object... args) {
        writeReport(level, format, stdout, args);
    }

    public void report(ReportLevel level, Exception e, String format, Object... args) {
        writeReport(level, format, stdout, args);
        writeReport(level, format, stderr, args);
        if (e != null && !isQuiet(level)) {
            e.printStackTrace(stderr);
        }
    }

    public void reportProperty(ReportLevel level, String key, Object value) {
        //do nothing...
        report(level, "%s: %s", key, value);
    }

    public void subreport(String subreportName, Runnable run) {
        run.run();
    }

}
