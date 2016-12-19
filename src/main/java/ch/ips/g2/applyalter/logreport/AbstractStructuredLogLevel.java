package ch.ips.g2.applyalter.logreport;

import ch.ips.g2.applyalter.ReportLevel;

import java.util.ArrayList;
import java.util.List;

/**
 * Report class that can consume log messages.
 */
abstract class AbstractStructuredLogLevel {

    private final List<String> messages = new ArrayList<String>();


    public List<String> getMessages() {
        return messages;
    }

    public void log(ReportLevel level, String message) {
        messages.add(message);
    }
}
