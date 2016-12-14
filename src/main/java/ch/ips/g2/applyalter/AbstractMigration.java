package ch.ips.g2.applyalter;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * @author Kamil Podlesak &lt;kamil.podlesak@ips-ag.cz&gt;
 * @version $Id$
 */
public abstract class AbstractMigration extends AbstractStatement {
    public static final long DEFAULT_STEP = 20000L;

    protected String logid;
    protected String description;
    protected Long maxblkcnt;
    protected Long step = DEFAULT_STEP;
    protected String placeholder;

    public String getLogid() {
        return logid;
    }

    public void setLogid(String logid) {
        this.logid = logid;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Maximum number of blocks (if positive). Negative number or zero means that
     * amount of migrated data is unlimited.
     */
    public Long getMaxblkcnt() {
        return maxblkcnt;
    }

    /**
     * Maximum number of blocks (if positive). Negative number or zero means that
     * amount of migrated data is unlimited.
     */
    public void setMaxblkcnt(Long maxblkcnt) {
        this.maxblkcnt = maxblkcnt;
    }

    public Long getStep() {
        return step;
    }

    public void setStep(Long step) {
        this.step = step;
    }

    public String getPlaceholder() {
        return placeholder;
    }

    public void setPlaceholder(String placeholder) {
        this.placeholder = placeholder;
    }

    @Override
    public void recordStructuredInfo(RunContext rctx) {
        super.recordStructuredInfo(rctx);
        rctx.reportProperty(ReportLevel.STATEMENT, "details", toString());
    }

    //-----------------------------------------------------------------------------------------------------------------
    //-----------------------------------------------------------------------------------------------------------------

    protected static class ProcessedQuery {
        final String statement;
        final int replacements;

        private ProcessedQuery(String statement, int replacements) {
            this.statement = statement;
            this.replacements = replacements;
        }
    }

    /**
     * Propeare SQL query: replace all occurences of {@link #getPlaceholder()} by
     *
     * @param rawStatement query to process, must contain placeholder
     * @param replacement  the replacement string
     * @return processed query
     */
    protected ProcessedQuery processQuery(String rawStatement, String replacement) {
        final String placeholder = getPlaceholder() == null ? getDefaultPlaceholder() : getPlaceholder();

        try {
            Pattern pattern = Pattern.compile(placeholder);

            Matcher matcher = pattern.matcher(rawStatement);
            StringBuffer sb = new StringBuffer();
            int counter = 0;
            while (matcher.find()) {
                matcher.appendReplacement(sb, replacement);
                counter++;
            }
            matcher.appendTail(sb);
            return new ProcessedQuery(sb.toString(), counter);
        } catch (PatternSyntaxException ignored) {
            throw new ApplyAlterException("invalid placeholder, not a valid regex: %s", placeholder);
        }
    }

    /**
     * Default placeholder, used when {@link #placeholder} is not filled.
     */
    protected abstract String getDefaultPlaceholder();


}
