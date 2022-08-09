package ch.ips.g2.applyalter;

import java.io.Closeable;
import java.io.IOException;

/**
 * Execution context, providing methods to output the results and report the processing steps.
 * Probably just writing to some standard output, but can be replaced
 * by something special (for example, to run ApplyAlter tool from JSP page).
 *
 * @author Kamil Podlesak &lt;kamil.podlesak@ips-ag.cz&gt;
 * @version $Id$
 */
public interface RunContext extends Closeable {

    /**
     * Run mode: this affect the whole execution.
     */
    RunMode getRunMode();

    /**
     * Is the incremental mode on?
     *
     * @return yes or no
     */
    boolean isIncremental();

    /**
     * Report activity to user. Uses {@link String#format(String, Object[])}  = printf.
     *
     * @param level  report level
     * @param format format string
     * @param args   arguments to format string
     */
    void report(ReportLevel level, String format, Object... args);

    /**
     * Report activity to user. Uses {@link String#format(String, Object[])}  = printf.
     *
     * @param level  report level
     * @param e      exception
     * @param format format string
     * @param args   arguments to format string
     */
    void report(ReportLevel level, Exception e, String format, Object... args);

    /**
     * Report structured property: add name=value pair. It should also write to standard log, to avoid duplicate info.
     *
     * @param level report level; used mainly if implementation also calls {@link #report(ReportLevel, String, Object...)}
     * @param key   key
     * @param value value, must be properly serializable; try to stick with primitive types and String
     */
    void reportProperty(ReportLevel level, String key, Object value);

    /**
     * Run some code in subreport; this method should execute try/finally block.
     *
     * @param subreportName name of the subreport; used to name xml element, should be simple word
     * @param run           code to execute; must be ALWAYS run!
     */
    void subreport(String subreportName, Runnable run);

    /**
     * Simple wrapper around {@link RunContext}: superclass for other context useful for composition.
     */
    class WrapperRunContext implements RunContext {

        private final RunContext wrapped;

        public WrapperRunContext(RunContext wrapped) {
            this.wrapped = wrapped;
        }

        public RunMode getRunMode() {
            return wrapped.getRunMode();
        }

        public boolean isIncremental() {
            return wrapped.isIncremental();
        }

        public void report(ReportLevel level, String format, Object... args) {
            wrapped.report(level, format, args);
        }

        public void report(ReportLevel level, Exception e, String format, Object... args) {
            wrapped.report(level, e, format, args);
        }

        public void reportProperty(ReportLevel level, String key, Object value) {
            wrapped.reportProperty(level, key, value);
        }

        public void subreport(String subreportName, Runnable run) {
            wrapped.subreport(subreportName, run);
        }

        public void close() throws IOException {
            wrapped.close();
        }
    }
}
