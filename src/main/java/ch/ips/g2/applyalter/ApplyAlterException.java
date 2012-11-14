package ch.ips.g2.applyalter;

import java.io.PrintStream;


/**
 * Exception during Alter script applying
 *
 * @author Martin Caslavsky &lt;martin.caslavsky@ips-ag.cz&gt;
 * @version $Id$
 */
@SuppressWarnings("serial")
public class ApplyAlterException extends RuntimeException {

    public ApplyAlterException() {
    }

    public ApplyAlterException(String message) {
        super(message);
    }

    public ApplyAlterException(String format, Object... param) {
        super(String.format(format, param));
    }

    public ApplyAlterException(Throwable cause) {
        super(cause);
    }

    public ApplyAlterException(String message, Throwable cause) {
        super(message, cause);
    }

    public ApplyAlterException(String format, Throwable cause, Object... param) {
        super(String.format(format, param), cause);
    }


    /**
     * Print error messages from chain of exceptions
     *
     * @param out where to print
     */
    public void printMessages(PrintStream out) {
        Throwable e = this;
        do {
            out.println(e.getMessage());
            e = e.getCause();
        } while (e != null);
    }

}
