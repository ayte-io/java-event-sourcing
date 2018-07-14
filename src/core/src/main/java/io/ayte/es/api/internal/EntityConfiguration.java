package io.ayte.es.api.internal;

import lombok.Builder;
import lombok.Data;

@Data
@Builder(toBuilder = true, builderClassName = "LombokBuilder")
@SuppressWarnings("squid:S1170")
public class EntityConfiguration {
    public static final EntityConfiguration DEFAULT = builder().build();

    /**
     * Limit of snapshots stored (older snapshots will be removed as
     * part of read repair). Negative values completely turn off this
     * behavior.
     */
    @Builder.Default
    private final long snapshotHistoryLength = -1;
    /**
     * Defines amount of events after which new snapshot may be automatically
     * created.
     */
    @Builder.Default
    private final long snapshotInterval = 25;
    /**
     * Probability (0..1) with which regular snapshot get() request will result
     * in additional check if this snapshot got outdated.
     * get() requests are never called automatically, so you as your own project
     * maintainer should know exactly where such calls are made.
     */
    @Builder.Default
    private final double snapshotCleanupProbability = 0.1;
    /**
     * Probability (0..1) with which entity get() request will try to recreate
     * missing snapshot if such gap is found. Absence of such snapshot doesn't
     * mean an error since it's presence can be checked while other process is
     * creating it, so value should be set according to amount of concurrent
     * requests for single entity.
     */
    @Builder.Default
    private final double snapshotReadRepairProbability = 0.5;
    @Builder.Default
    private final double eventRepairProbability = 0.5;
    /**
     * Whether or not repair action exception will be propagated to
     * calling code.
     */
    @Builder.Default
    private final boolean suppressRepairErrors = false;
    /**
     * Whether or not listener action exception will be propagated to
     * calling code.
     */
    @Builder.Default
    private final boolean suppressListenerErrors = false;
    /**
     * Defines if control will be returned to calling code only after
     * all listeners have finished or not.
     */
    @Builder.Default
    private final boolean awaitListeners = true;
    /**
     * Whether or not engine is allowed to generate snapshots as a part
     * of entity read request.
     */
    @Builder.Default
    private final boolean repairSnapshotOnReads = false;
}
