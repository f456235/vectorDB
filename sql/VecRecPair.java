package org.vanilladb.core.sql;
import org.vanilladb.core.storage.record.RecordId;

public class VecRecPair {
    private VectorConstant vec;
    private RecordId rid;

    public VecRecPair(VectorConstant vec, RecordId rid) {
        this.vec = vec;
        this.rid = rid;
    }

    public VectorConstant getVec() {
        return vec;
    }

    public RecordId getRid() {
        return rid;
    }
}
