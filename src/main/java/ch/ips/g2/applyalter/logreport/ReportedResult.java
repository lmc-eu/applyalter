package ch.ips.g2.applyalter.logreport;

/**
 * Simple result of alters and statements.
 */
public enum ReportedResult {

    /**
     * Finished, success.
     */
    FINISHED,
    /**
     * Failed, end of execution.
     */
    FAILED,
    /**
     * Failed, but failure is ignored.
     */
    FAILED_IGNORED,
    /**
     * Skipped - no attempt to execute.
     */
    SKIPPED

}
