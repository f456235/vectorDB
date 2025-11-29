package org.vanilladb.core.sql;


import org.vanilladb.core.storage.record.RecordId;
import org.vanilladb.core.sql.distfn.DistanceFn;

public class VecRecPairComp implements Comparable<VecRecPairComp> {
    private VecRecPair vr;
    private DistanceFn distFn;
    private double distToTarget = -1;

    public VecRecPairComp(VecRecPair vr, DistanceFn distFn) {
        this.vr = vr;
        this.distFn = distFn;
    }

    public VectorConstant getVec() {
        return vr.getVec();
    }

    public RecordId getRid() {
        return vr.getRid();
    }

    @Override
    public int compareTo(VecRecPairComp other) {
        if (distToTarget == -1)
            distToTarget = distFn.distance(this.getVec());
        if (other.distToTarget == -1)
            other.distToTarget = distFn.distance(other.getVec());
        return Double.compare(distToTarget, other.distToTarget);
    }

    public RecordId getRecordId() {
        return vr.getRid();
    }

}
