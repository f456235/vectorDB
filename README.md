# 🚀 High-Performance Vector Retrieval System

## 🌟 Project Overview

This project implements the **IVF_FLAT (Inverted File with Flat Index)** indexing structure within a database system to optimize the performance of **Nearest Neighbor Search (NNS)**. We focus on leveraging **K-means Clustering** for preprocessing vector data and utilizing **SIMD (Single Instruction, Multiple Data)** techniques to accelerate vector distance computation. This approach significantly boosts both **Recall** and **Throughput** on large-scale vector datasets, such as the SIFT benchmark.

## ⚙️ Core Implementation Details

### 1. IVF_FLAT Index Structure

* **Indexing Idea:** Perform K-means clustering after loading the testbed. Establish a centroid table and a cluster table (recording vectors in each cluster). Use IVF_FLAT as the index.
* **Query Execution:** `NearestNeighborPlan` calls the vector-specific `IVPlan`, which then calls `IVScan` utilizing the `IVF_FLATIndex`.
* **Index Building:** Done within `SiftTestbedLoaderProc`'s `generateItems()`.

### 2. K-means Clustering

1.  **Initialization:** Select K arbitrary `VectorRecordPairs` as initial centroids.
2.  **Optimization Loop (MaxIter times):**
    * Assign all `VectorRecordPairs` to their closest cluster.
    * Update all cluster centroids to the average of their assigned vectors.
3.  **Storage:** Cluster data is stored by the IVF Flat Index, recording the centroid's `VectorConstant` and the list of vectors.

### 3. IVPlan and IVScan

* **IVPlan:** Replaces `SortPlan` in `NearestNeighborPlan`. Passes the index to `IVScan`.
* **IVScan - `beforeFirst()`:**
    * Passes the distance function (`DistanceFn`) to `IVF_FLATIndex`.
    * Receives a sorted **centroid priority queue**, which maintains distance order with lower computational cost.

### 4. IVF_FLATIndex Optimizations

* **Vector Comparison (`VecRecPairComp`):** Implemented to compare vector records in the heap.
    * Initializes `dist` to $-1$. Subsequent calls check this value to decide whether to call `distanceTo` (avoiding redundant distance calculations).
    * Avoids $sqrt(x)$ calculation by sorting based on **squared distance** due to monotonicity.
* **`preLoadToMemory`:** Reads `idx_items_centroid` table into `listCentroids`.
* **`beforeFirst(DistanceFn distFn)`:** Loads centroids and sorts them into a priority queue based on their distance to the query vector.

### 5. SIMD Acceleration

* **Principle:** Use the monotonicity of $sqrt(x)$ to sort by squared Euclidean distance.
* **Implementation:** Vector dimensions are loaded into `jdk.incubator.FloatVector` for SIMD operations (`add`, `sub`, `mul`).
* **Optimization:** Loads `species.length()` elements at once via `FloatVector.fromArray(...)`, replacing the dimension-by-dimension `get(i)` approach.
* **Tail Handling:** Processes the tail elements from `species.loopBound(vec.dimension())` up to `vec.dimension()` using regular processing.

### 6. Benchmark Insert Control

* **IndexUpdatePlanner:** Uses a `static boolean variable benchingState` to control the `executeInsert()` process.
    * `SiftTestbedLoaderProc`: Sets `benchingState` to `false` (standard index build).
    * `SiftInsertProc`: Sets `benchingState` to `true` (uses `IVF_FLATIndex`'s insert for benchmark).

## 🧪 Experimental Results and Analysis

### Environment

| Component | Specification |
| :--- | :--- |
| **CPU** | Intel(R) Core(TM) i5-1135G7 @ 2.40GHz |
| **RAM** | 16 GB |
| **Storage** | 512 GB SSD |
| **OS** | Windows 11 |
| **Benchmark** | SIFT Benchmark |

### Recall Comparison

* **Fixed Centroid Count, Increased Cluster Count:** Recall **may not increase** because reduced data per cluster and insufficient search coverage lead to potential data omission (especially near boundaries).
* **Fixed Cluster Count, Increased Centroid Count:** Recall **significantly improves** because increasing the number of clusters to search increases the probability of finding target data.

### Throughput Comparison

* **Centroid Number = 1, Increased Cluster Count:** Throughput increases but plateaus (small difference between 800 and 1600 clusters).
* **Centroid Number = 5, Increased Cluster Count:** Throughput continues to increase, indicating higher cluster counts effectively reduce the number of irrelevant vectors scanned.
* **Fixed Cluster Count, Increased Centroid Number:** Throughput **decreases** due to the need to scan more clusters.

### Combined Recall x Throughput Trade-off

The overall performance ranking for the (Cluster Count, Centroid Count) configurations is:

$$(1600, 5) > (1600, 1) > (800, 1) > (800, 5) > (200, 1) > (200, 5)$$
