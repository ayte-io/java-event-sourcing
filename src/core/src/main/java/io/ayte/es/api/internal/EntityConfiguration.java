package io.ayte.es.api.internal;

public interface EntityConfiguration {
    long getSnapshotHistoryLength();
    long getSnapshotInterval();
    double getSnapshotRepairProbability();
    double getEventRepairProbability();
}
