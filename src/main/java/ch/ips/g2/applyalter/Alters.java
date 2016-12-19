package ch.ips.g2.applyalter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

/**
 * Holder for loaded alterscripts, with composite hash of loaded sources.
 */
public class Alters {
    @Nonnull
    final List<Alter> alters;
    @Nullable
    final String sourceHash;

    Alters(@Nonnull List<Alter> alters, @Nullable String sourceHash) {
        this.alters = alters;
        this.sourceHash = sourceHash;
    }

    @Nonnull
    public List<Alter> getAlters() {
        return alters;
    }

    /**
     * Source hash: when not present, these alterscripts are internal.
     */
    @Nullable
    public String getSourceHash() {
        return sourceHash;
    }
}
