package org.vanilladb.core.storage.index;

import org.vanilladb.core.server.VanillaDb;
import org.vanilladb.core.sql.BigIntConstant;
import org.vanilladb.core.sql.IntegerConstant;
import org.vanilladb.core.sql.VectorConstant;
import org.vanilladb.core.sql.distfn.DistanceFn;
import org.vanilladb.core.sql.distfn.EuclideanFn;
import org.vanilladb.core.storage.buffer.Buffer;
import org.vanilladb.core.storage.file.BlockId;

import org.vanilladb.core.storage.metadata.TableInfo;
import org.vanilladb.core.storage.metadata.index.IndexInfo;
import org.vanilladb.core.storage.record.RecordFile;
import org.vanilladb.core.storage.record.RecordId;
import org.vanilladb.core.storage.record.RecordPage;
import org.vanilladb.core.storage.tx.Transaction;
import org.vanilladb.core.util.CoreProperties;
import org.vanilladb.core.sql.Schema;
import org.vanilladb.core.sql.VecRecPair;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class IVF_FLATIndex extends Index {
    public static final String SCHEMA_KEY = "data_vector", SCHEMA_RID_BLOCK = "rid_block", 
                             SCHEMA_RID_ID = "rid_id", SCHEMA_CENTROID_NUM = "centroid_id";
    private static List<VecIntPair> listCentroids = null;
    private PriorityQueue<VecIntPair> pqCentroids;
    private RecordFile dataFile;
    private Transaction tx;
    private IndexInfo ii;
    private int K; // Number of centroids to scan
    private int cur_k;
    private boolean isBeforeFirsted;
    private DistanceFn distFn;


    public static final int NUM_CLUSTERS;
    public static final double RATE;

	static {
		NUM_CLUSTERS = CoreProperties.getLoader().getPropertyAsInteger(
				IVF_FLATIndex.class.getName() + ".NUM_CLUSTERS", 200);
	}
    static {
		RATE = CoreProperties.getLoader().getPropertyAsDouble(
				IVF_FLATIndex.class.getName() + ".RATE", 0.1);
	}
    public static class VecIntPair implements Comparable<VecIntPair> {
        VectorConstant v;
        int fileId;
        private double distToTarget;

        public VecIntPair(VectorConstant v, int fileId) {
            this.v = v;
            this.fileId = fileId;
            this.distToTarget = -1;
        }

        public void setDistFn(DistanceFn distFn) {
            this.distToTarget = distFn.distance(this.v);
        }

        @Override
        public int compareTo(VecIntPair other) {
            return Double.compare(this.distToTarget, other.distToTarget);
        }
    }

    public IVF_FLATIndex(IndexInfo ii, SearchKeyType keyType, Transaction tx) {
        super(ii, keyType, tx);
        this.ii = ii;
        this.tx = tx;
        this.K = 1;// Default K, will be overridden by openIVF
        this.cur_k = -1;
        this.isBeforeFirsted = false;
    }


    public static long searchCost(SearchKeyType keyType, long totRecs, long matchRecs) {
		int rpb = Buffer.BUFFER_SIZE / RecordPage.slotSize(schema(keyType));
		return (totRecs / rpb) / NUM_CLUSTERS;
	}
    // // Method to support IVPlan's openIVF call
    // public static IVF_FLATIndex openIVF(Transaction tx, IndexInfo ii, int K) {
    //     IVF_FLATIndex index = new IVF_FLATIndex(ii, SearchKeyType.VECTOR, tx);
    //     index.K = K; // Set number of centroids to scan
    //     return index;
    // }

    private static String keyFieldName(int index) {
		return SCHEMA_KEY + index;
	}

    private static Schema schema(SearchKeyType keyType) {
		Schema sch = new Schema();
		for (int i = 0; i < keyType.length(); i++)
			sch.addField(keyFieldName(i), keyType.get(i));
		sch.addField(SCHEMA_RID_BLOCK, org.vanilladb.core.sql.Type.BIGINT);
		sch.addField(SCHEMA_RID_ID, org.vanilladb.core.sql.Type.INTEGER);
		return sch;
	}

    @Override
    public void preLoadToMemory() {
        if (listCentroids != null) return;

        String tblname = "idx_items_centroid";
        System.out.println("Loading centroids from table: " + tblname);
        TableInfo ti = new TableInfo(tblname, centroidsSchema());
        RecordFile rf = ti.open(tx, true);

        long size = VanillaDb.fileMgr().size(ti.fileName());
        BlockId blk;
        List<Buffer> buffers = new ArrayList<>();

        for (int i = 0; i < size; ++i) {
            blk = new BlockId(tblname, i);
            buffers.add(tx.bufferMgr().pin(blk));
        }

        rf.beforeFirst();
        listCentroids = new CopyOnWriteArrayList<>();

        while (rf.next()) {
            VectorConstant v = (VectorConstant) rf.getVal(SCHEMA_KEY);
            int num = (int) rf.getVal(SCHEMA_CENTROID_NUM).asJavaVal();
            listCentroids.add(new VecIntPair(v, num));
        }

        rf.close();
        buffers.forEach(b -> tx.bufferMgr().unpin(b));
    }
    
    @Override
    public void beforeFirst(DistanceFn distFn) {
        preLoadToMemory();
        
        /* sort the clusters according to the 
            distance between query vector and cluster point */
        this.pqCentroids = new PriorityQueue<>();
        listCentroids.forEach(c -> {
            c.setDistFn(distFn);
            this.pqCentroids.add(c);
        });

        isBeforeFirsted = true;
    }

    public boolean next() {
        if (!isBeforeFirsted)
            throw new IllegalStateException("You must call beforeFirst() before iterating index '"
                    + ii.indexName() + "'");

        while (dataFile == null || !dataFile.next()) {
            // End case: last centroid is iterated
            if (cur_k + 1 == K)
                return false;
            // close traversed dataFile
            if (dataFile != null)
                dataFile.close();
            // load new data file from FileSystem
            ++cur_k;
            var cur_cent = pqCentroids.poll();
            //System.out.println("index info:" + ii.indexName() + ", current centroid: " + cur_cent.fileId);
            var ti = new TableInfo("idx_items_data" +
                    cur_cent.fileId, dataSchema());
            dataFile = ti.open(tx, true);
            dataFile.beforeFirst();
        }
        return true;
    }

    @Override
    public RecordId getDataRecordId() {
        if (dataFile == null) {
            throw new IllegalStateException("No data file is currently being iterated. Call next() first.");
        }
        long blockNum = (long) ((BigIntConstant) dataFile.getVal(SCHEMA_RID_BLOCK)).asJavaVal();
        int recordId = (int) ((IntegerConstant) dataFile.getVal(SCHEMA_RID_ID)).asJavaVal();
        return new RecordId(new BlockId("sift", blockNum), recordId);
    }

    // Method to support IndexVecScan
    public VecRecPair getDataVecRecPair() {
        if (dataFile == null) {
            throw new IllegalStateException("No data file is currently being iterated. Call next() first.");
        }
        VectorConstant vec = (VectorConstant) dataFile.getVal(SCHEMA_KEY);
        long blockNum = (long) ((BigIntConstant) dataFile.getVal(SCHEMA_RID_BLOCK)).asJavaVal();
        int recordId = (int) ((IntegerConstant) dataFile.getVal(SCHEMA_RID_ID)).asJavaVal();
        RecordId rid = new RecordId(new BlockId("sift", blockNum), recordId);
        return new VecRecPair(vec, rid);
    }

        public void insert(VectorConstant key, RecordId dataRecordId) {
        preLoadToMemory();
        
        // set the disfn with insert vector
        var keyDist = new EuclideanFn("");
        keyDist.setQueryVector(key);
        
        // loop through all the centroids to find the closest one
        int idx = -1;
        double dist = Double.MAX_VALUE, tmp;
        for (int i = 0; i < listCentroids.size(); ++i) {
            if ((tmp = keyDist.distance(listCentroids.get(i).v)) < dist) {
                idx = i;
                dist = tmp;
            }
        }
        
        // open the corresponding data file and insert the vector
        String tblname = "idx_items_data" + listCentroids.get(idx).fileId;
        TableInfo ti = new TableInfo(tblname, dataSchema());
        RecordFile rf = ti.open(tx, true);
        rf.insert();
        rf.setVal(SCHEMA_KEY, key);
        rf.setVal(SCHEMA_RID_BLOCK, new BigIntConstant(dataRecordId.block().number()));
        rf.setVal(SCHEMA_RID_ID, new IntegerConstant(dataRecordId.id()));
        rf.close();
    }

    @Override
    public void delete(SearchKey key, RecordId dataRecordId, boolean doLogicalLogging) {
        // Deletion not implemented in original; provide basic implementation
        if (!(key.get(0) instanceof VectorConstant)) {
            throw new IllegalArgumentException("SearchKey must be a VectorConstant");
        }
        VectorConstant vectorKey = (VectorConstant) key.get(0);

        DistanceFn keyDist = new EuclideanFn("");
        keyDist.setQueryVector(vectorKey);

        int idx = -1;
        double dist = Double.MAX_VALUE, tmp;

        for (int i = 0; i < listCentroids.size(); ++i) {
            if ((tmp = keyDist.distance(listCentroids.get(i).v)) < dist) {
                idx = i;
                dist = tmp;
            }
        }

        String tblname = "idx_items_data" + listCentroids.get(idx).fileId;
        TableInfo ti = new TableInfo(tblname, dataSchema());
        RecordFile rf = ti.open(tx, true);

        rf.beforeFirst();
        while (rf.next()) {
            VectorConstant vec = (VectorConstant) rf.getVal(SCHEMA_KEY);
            long blockNum = (long) ((BigIntConstant) rf.getVal(SCHEMA_RID_BLOCK)).asJavaVal();
            int recordId = (int) ((IntegerConstant) rf.getVal(SCHEMA_RID_ID)).asJavaVal();
            if (vec.equals(vectorKey) && blockNum == dataRecordId.block().number() && recordId == dataRecordId.id()) {
                rf.delete();
                break;
            }
        }
        rf.close();
    }

    @Override
    public void close() {
        if (dataFile != null) {
            dataFile.close();
            dataFile = null;
        }
        pqCentroids = null;
        cur_k = -1;
        isBeforeFirsted = false;
    }

    private Schema centroidsSchema() {
        Schema schema = new Schema();
        schema.addField(SCHEMA_KEY, org.vanilladb.core.sql.Type.VECTOR(128));
        schema.addField(SCHEMA_CENTROID_NUM, org.vanilladb.core.sql.Type.INTEGER);
        return schema;
    }

    private Schema dataSchema() {
        Schema schema = new Schema();
        schema.addField(SCHEMA_KEY, org.vanilladb.core.sql.Type.VECTOR(128));
        schema.addField(SCHEMA_RID_BLOCK, org.vanilladb.core.sql.Type.BIGINT);
        schema.addField(SCHEMA_RID_ID, org.vanilladb.core.sql.Type.INTEGER);
        return schema;
    }

    @Override
    public void beforeFirst(SearchRange searchRange) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'beforeFirst'");
    }

    @Override
    public void insert(SearchKey key, RecordId dataRecordId, boolean doLogicalLogging) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'insert'");
    }

}