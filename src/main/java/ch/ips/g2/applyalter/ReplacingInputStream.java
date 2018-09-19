package ch.ips.g2.applyalter;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

/**
 * <p>
 * Replace parameter in input stream data.
 * Parameter format is {{paramName}}.
 * Supported characters in parameter name are [a-zA-Z0-9_\-].
 * </p>
 * <p>
 * Inspired by https://gist.github.com/lhr0909/e6ac2d6dd6752871eb57c4b083799947
 * </p>
 */
public class ReplacingInputStream extends InputStream {
    private static final byte[] PARAM_SEPARATOR_PREFIX = new byte[]{(byte) '{', (byte) '{'};
    private static final byte[] PARAM_SEPARATOR_SUFFIX = new byte[]{(byte) '}', (byte) '}'};
    /**
     * Supported bytes for parameter name [\-0-9a-zA-Z_]. It must be sorted!
     * Arrays.binarySearch is used to check if byte is supported as name parameter.
     */
    private static final byte[] PARAM_CHARS_SUPPORTED = ("-0123456789" +
            "ABCDEFGHIJKLMNOPQRSTUVWXYZ_" +
            "abcdefghijklmnopqrstuvwxyz").getBytes();

    private final InputStream sourceInputStream;
    private final Map<String, byte[]> params;
    private final Queue<Integer> inQueue, outQueue;

    /**
     * @param sourceInputStream source input stream.
     * @param params            map of parameter. Key is the parameter name. Value is replacement content.
     */
    public ReplacingInputStream(InputStream sourceInputStream, @Nonnull Map<String, byte[]> params) {
        super();
        this.sourceInputStream = sourceInputStream;
        this.params = params;
        this.inQueue = new LinkedList<>();
        this.outQueue = new LinkedList<>();
    }

    private int readOneInQueue() throws IOException {
        int next = sourceInputStream.read();
        inQueue.offer(next);
        return next;
    }

    private void readAhead() throws IOException {
        while (inQueue.size() < PARAM_SEPARATOR_PREFIX.length) {
            if (readOneInQueue() == -1) {
                return;
            }
        }
    }

    private boolean matchStartingSeparator() {
        final Iterator<Integer> iterator = inQueue.iterator();

        for (byte b : PARAM_SEPARATOR_PREFIX) {
            if (!iterator.hasNext() || b != iterator.next()) {
                return false;
            }
        }
        return true;
    }

    private boolean readParameterNameIntoInQueue() throws IOException {
        // 1. read one by one byte into inQueue until byte is supported character of parameter name or end of stream
        int next;
        do {
            next = readOneInQueue();
        } while (next != -1 &&
                Arrays.binarySearch(PARAM_CHARS_SUPPORTED, (byte) next) >= 0);
        // 2. if character of parameter name is not supported - check if it is parameter suffix.
        return checkAndReadUntilEndOfSeparator(next, 0);
    }

    private boolean checkAndReadUntilEndOfSeparator(final int current, int sepIdx) throws IOException {
        if (PARAM_SEPARATOR_SUFFIX[sepIdx] == current) {
            if (sepIdx == PARAM_SEPARATOR_SUFFIX.length - 1) {
                return true;
            }
            return checkAndReadUntilEndOfSeparator(readOneInQueue(), ++sepIdx);
        }
        return false;
    }

    private String getParameterName() {
        if (inQueue.size() == PARAM_SEPARATOR_PREFIX.length + PARAM_SEPARATOR_SUFFIX.length) {
            return null;
        }
        // remove prefix separator from InQueue
        removeInQueue(PARAM_SEPARATOR_PREFIX.length);

        final StringBuilder sb = new StringBuilder();
        final int size = inQueue.size() - PARAM_SEPARATOR_PREFIX.length;
        for (int i = 0; i < size; i++) {
            sb.append((char) inQueue.remove().intValue());
        }

        // remove suffix separator from InQueue
        removeInQueue(PARAM_SEPARATOR_SUFFIX.length);
        return sb.toString();
    }

    private void removeInQueue(int cnt) {
        for (int i = 0; i < cnt; i++) {
            inQueue.remove();
        }
    }

    /**
     * Read ahead some bytes into inQueue and get parameter name if pattern matched.
     *
     * @return parameter name or null if no parameter was read.
     */
    private String readAheadGetParameter() throws IOException {
        readAhead();
        if (matchStartingSeparator() && readParameterNameIntoInQueue()) {
            return getParameterName();
        } else {
            return null;
        }
    }

    @Override
    public int read() throws IOException {
        while (outQueue.isEmpty()) {
            final String paramName = readAheadGetParameter();

            if (paramName != null) {
                final byte[] replacement = params.get(paramName);
                if (replacement == null) {
                    throw new IllegalStateException("Unknown parameter: " + paramName);
                }
                for (byte b : replacement) {
                    outQueue.offer((int) b);
                }
            } else {
                outQueue.add(inQueue.remove());
            }
        }
        return outQueue.remove();
    }

    @Override
    public void close() throws IOException {
        super.close();
        sourceInputStream.close();
    }
}
