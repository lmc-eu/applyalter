package ch.ips.g2.applyalter;

import java.io.IOException;

/**
 * Internal run context: used to run internal alterscript. Nothing should be printed to user.
 *
 * @author Kamil Podlesak &lt;kamil.podlesak@ips-ag.cz&gt;
 * @version $Id$
 */
class InternalRunContext implements RunContext {
    public RunMode getRunMode() {
        return RunMode.SHARP;
    }

    /**
     * Internal alterscripts are always executed straight on.
     */
    public boolean isIncremental() {
        return false;
    }

    public void report(ReportLevel level, String format, Object... args) {
        //skip
    }

    public void report(ReportLevel level, Exception e, String format, Object... args) {
        //skip
    }

    public void reportProperty(ReportLevel level, String key, Object value) {
    }

    public void subreport(String subreportName, Runnable run) {
        //do nothing except execution
        run.run();
    }

    public void close() throws IOException {
    }
}
