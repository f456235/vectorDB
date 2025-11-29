package org.vanilladb.core.sql;


import java.util.ArrayList;
import java.util.List;


public class Cluster {
    
    private VectorConstant centroid;
    private List<VecRecPair> data;

    Cluster(VectorConstant centroid){
        this.centroid = centroid;
        this.data = new ArrayList<>();
    }

    public void addData(VecRecPair vecRecPair){
        data.add(vecRecPair);
    }

    public void clearData(){
        data.clear();
    }

    public void setCentroid(VectorConstant centroid){
        this.centroid = centroid;
    }

    public VectorConstant getCentroid(){
        return centroid;
    }

    public List<VecRecPair> getData(){
        return data;
    }

    public void reComputeCentroid(){
        if (data.size() == 0) return;
        VectorConstant newCentroid = data.get(0).getVec();
        for (int i=1; i < data.size(); i++) {
            VectorConstant vec = data.get(i).getVec();
            newCentroid = (VectorConstant)newCentroid.add(vec);
        }
        

        newCentroid = (VectorConstant) newCentroid.div(new IntegerConstant(data.size()));
    }
}
