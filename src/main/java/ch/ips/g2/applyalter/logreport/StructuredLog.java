package ch.ips.g2.applyalter.logreport;

import ch.ips.g2.applyalter.RunContext;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;

import static ch.ips.g2.applyalter.ReportLevel.*;

/**
 * @author Kamil Podlesak &lt;kamil.podlesak@lmc.eu&gt;
 */
public class StructuredLog {

    /**
     * Utility method: open file for writing and back up and old one when present. Error are logged, but ignored - this
     * method does <b>NOT</b> throws any {@link java.io.IOException}!
     *
     * @param rctx     run context - used to report error
     * @param filePath file to open
     * @return output stream; <code>null</code> when that file cannot be made open
     */
    @Nullable
    public static OutputStream openLogFile(@Nonnull RunContext rctx, @Nonnull String filePath) {
        final File logfile = new File(filePath);
        rctx.report(MAIN, "structured log file: %s", logfile);
        if (logfile.exists()) {
            final File backup = new File(filePath + ".backup." + System.currentTimeMillis());
            if (logfile.renameTo(backup)) {
                rctx.report(MAIN, "old structured log backed up as: %s", backup);
            } else {
                rctx.report(ERROR, "cannot backup, writing over old structured log");
            }
        }
        try {
            return new FileOutputStream(logfile, false);
        } catch (FileNotFoundException e) {
            rctx.report(ERROR, e, "failed to open log file for writing: %s", logfile);
            return null;
        }
    }

    /**
     * Create run context with json output.
     *
     * @param fos  output stream
     * @param rctx run context to wrap - this is still used for standard output
     * @return new run context, that also writes to structured log.
     */
    public static RunContext createJson(OutputStream fos, RunContext rctx) {
        return new JsonLoggingRunContext(rctx, fos);
    }
}
