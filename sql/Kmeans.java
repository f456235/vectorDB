package org.vanilladb.core.sql;
import java.util.List;
import java.util.ArrayList;
import java.util.Random;

public class Kmeans {
    private int k;
    private List<Cluster> clusters;
    private List<VecRecPair> data;

    public Kmeans(int k, List<VecRecPair> data) {

        System.out.println("Kmeans initialized with k = " + k + ", data size = " + data.size());

        this.k = k;
        this.data = data;
        this.clusters = new ArrayList<>();
    }

    public List<Cluster> KMeansPlusPlus(int maxIter){
        
        initializeRandomCentroids();

        for (int i = 0; i < maxIter; i++) {

            System.out.println("Iteration " + (i + 1) + " of KMeans++...");

            for (Cluster c: clusters) c.clearData();

            // step 1: assign each point to the closest centroid
            for (VecRecPair p : data) {
                Cluster c = assignCluster(p);
                c.addData(p);
            }

            // step 2: update the centroids
            for (Cluster c : clusters) {
                
                c.reComputeCentroid();  
            }

        }

        return clusters;
    }


    // for k-menas++
    private void intializeCentroids(){

        System.out.println("Initializing centroids using KMeans++...");

        Random random = new Random();

        clusters.clear();
        
        // step 1: choose a random point as the first centroid

        VecRecPair first = data.get(random.nextInt(data.size()));
        clusters.add(new Cluster(first.getVec()));

        // step 2: choose the next k-1 centroids by distance

        while(clusters.size() < k) {
            
            if (clusters.size() % 10 == 0) {
                System.out.println("Already chose " + clusters.size() + "th centroid\n");
            }

            

            List<Double> distances = new ArrayList<>();
            double sum = 0;

            for (VecRecPair p : data) {
                double minDistance = Double.MAX_VALUE;
                for (Cluster c : clusters) {
                   double d = p.getVec().distance(c.getCentroid());
                   minDistance = Math.min(minDistance, d);
                }
                double w = Math.pow(minDistance, 2);
                distances.add(w);
                sum += w;
            }

            double r = random.nextDouble() * sum;
            double total = 0;
            for (int i = 0; i < distances.size(); i++) {
                total += distances.get(i);
                if (total >= r) {
                    clusters.add(new Cluster(data.get(i).getVec()));
                    break;
                }
            }
        }

    }
    
    // for k-means intialization (not k-means++)
    private void initializeRandomCentroids() {
        System.out.println("Initializing centroids randomly...");

        Random random = new Random();
        clusters.clear();

        for (int i = 0; i < k; i++) {
            VecRecPair randomPair = data.get(random.nextInt(data.size()));
            clusters.add(new Cluster(randomPair.getVec()));
        }
    }

    private Cluster assignCluster(VecRecPair p){
        Cluster closest = null;
        double minDistance = Double.MAX_VALUE;

        for (Cluster c : clusters) {
            double d = p.getVec().distance(c.getCentroid());
            if (d < minDistance) {
                minDistance = d;
                closest = c;
            }
        }

        return closest;
    }
    
    public List<Cluster> getClusters() {
        return clusters;
    }
}
