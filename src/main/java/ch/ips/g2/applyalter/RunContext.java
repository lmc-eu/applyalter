package ch.ips.g2.applyalter;

/**
 * Execution context, providing methods to output the results and report the processing steps.
 * Probably just writing to some standard output, but can be replaced
 * by something special (for example, to run ApplyAlter tool from JSP page).
 *
 * @author Kamil Podlesak &lt;kamil.podlesak@ips-ag.cz&gt;
 * @version $Id$
 */
public interface RunContext {

    /**
     * Run mode: this affect the whole execution.
     */
    RunMode getRunMode();

    /**
     * Is the incremental mode on?
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


}
