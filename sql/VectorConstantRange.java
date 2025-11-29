package org.vanilladb.core.sql;

import org.vanilladb.core.sql.distfn.DistanceFn;
import org.vanilladb.core.storage.metadata.statistics.Histogram;

public class VectorConstantRange extends ConstantRange {
    private DistanceFn distFn;
    private double radius;

    public VectorConstantRange(DistanceFn distFn, double radius) {
        if (distFn == null)
            throw new IllegalArgumentException("Distance function cannot be null");
        this.distFn = distFn;
        this.radius = radius;
    }

    // 因為 ConstantRange 定義的是 contains(Constant)
    // 你要 override 且做類型轉換才能使用 VectorConstant
    @Override
    public boolean contains(Constant c) {
        if (!(c instanceof VectorConstant))
            return false;
        VectorConstant vec = (VectorConstant) c;
        double dist = distFn.distance(vec);
        return dist <= radius;
    }

    public double getRadius() {
        return radius;
    }

    public void setRadius(double radius) {
        this.radius = radius;
    }

    @Override
    public boolean isValid() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'isValid'");
    }

    @Override
    public boolean hasLowerBound() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'hasLowerBound'");
    }

    @Override
    public boolean hasUpperBound() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'hasUpperBound'");
    }

    @Override
    public Constant low() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'low'");
    }

    @Override
    public Constant high() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'high'");
    }

    @Override
    public boolean isLowInclusive() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'isLowInclusive'");
    }

    @Override
    public boolean isHighInclusive() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'isHighInclusive'");
    }

    @Override
    public double length() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'length'");
    }

    @Override
    public ConstantRange applyLow(Constant c, boolean inclusive) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'applyLow'");
    }

    @Override
    public ConstantRange applyHigh(Constant c, boolean incl) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'applyHigh'");
    }

    @Override
    public ConstantRange applyConstant(Constant c) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'applyConstant'");
    }

    @Override
    public boolean isConstant() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'isConstant'");
    }

    @Override
    public Constant asConstant() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'asConstant'");
    }

    @Override
    public boolean lessThan(Constant c) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'lessThan'");
    }

    @Override
    public boolean largerThan(Constant c) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'largerThan'");
    }

    @Override
    public boolean isOverlapping(ConstantRange r) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'isOverlapping'");
    }

    @Override
    public boolean contains(ConstantRange r) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'contains'");
    }

    @Override
    public ConstantRange intersect(ConstantRange r) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'intersect'");
    }

    @Override
    public ConstantRange union(ConstantRange r) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'union'");
    }
}