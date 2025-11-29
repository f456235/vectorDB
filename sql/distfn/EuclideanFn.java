package org.vanilladb.core.sql.distfn;

import org.vanilladb.core.sql.VectorConstant;

import jdk.incubator.vector.*;

public class EuclideanFn extends DistanceFn {

    public EuclideanFn(String fld) {
        super(fld);
    }

    @Override
    protected double calculateDistance(VectorConstant vec) {
        /*double sum = 0;
        for (int i = 0; i < vec.dimension(); i++) {
            double diff = query.get(i) - vec.get(i);
            sum += diff * diff;
        }
        return Math.sqrt(sum);*/

        VectorSpecies<Float> species = FloatVector.SPECIES_PREFERRED;
        
        int i = 0;
        double sum = 0;

        float[] a_query = query.getVec(), a_vec = vec.getVec();

        for (;i < species.loopBound(vec.dimension()); i+=species.length()) {
            FloatVector v_query = FloatVector.fromArray(species, a_query, i), 
                        v_vec = FloatVector.fromArray(species, a_vec, i);
            
            FloatVector diff = v_query.sub(v_vec);
            FloatVector diff_sqr = diff.mul(diff);

            sum += diff_sqr.reduceLanes(VectorOperators.ADD);
        }
        
        for (; i < vec.dimension(); i++) {
            double diff = query.get(i) - vec.get(i);
            sum += diff * diff;
        }

        return sum;
    }
    
}
