package id.ac.ui.cs.prices.winvmj.composer.ui.wizards.models;

import java.util.*;
import java.util.stream.Collectors;

/**
 * NFR KNN3 - K-Nearest Neighbors (K=3) algorithm for cloud instance recommendation
 * Based on workload characteristics (TPS and transactions) with Apdex score filtering
 * 
 * Data from Table 5.1 - includes both AWS and GCP instances
 */
public class NFRKnn3 {
    
    // Dataset from Table 5.1 - Complete data including AWS and GCP
    private static final List<CloudConfiguration> CLOUD_DATA = Arrays.asList(
        // AWS t2.small, ap-southeast-1 (Singapore)
        new CloudConfiguration(10, 1000, "t2.small", "ap-southeast-1", 1.000, "AWS"),
        new CloudConfiguration(10, 2000, "t2.small", "ap-southeast-1", 0.920, "AWS"),
        new CloudConfiguration(10, 4000, "t2.small", "ap-southeast-1", 0.710, "AWS"),
        new CloudConfiguration(10, 8000, "t2.small", "ap-southeast-1", 0.510, "AWS"),
        new CloudConfiguration(10, 16000, "t2.small", "ap-southeast-1", 0.200, "AWS"),
        new CloudConfiguration(100, 1000, "t2.small", "ap-southeast-1", 0.446, "AWS"),
        new CloudConfiguration(100, 2000, "t2.small", "ap-southeast-1", 0.173, "AWS"),
        new CloudConfiguration(100, 4000, "t2.small", "ap-southeast-1", 0.039, "AWS"),
        new CloudConfiguration(100, 8000, "t2.small", "ap-southeast-1", 0.008, "AWS"),
        new CloudConfiguration(100, 16000, "t2.small", "ap-southeast-1", 0.004, "AWS"),

        // AWS t2.small, us-west-2 (Oregon)
        new CloudConfiguration(10, 1000, "t2.small", "us-west-2", 0.940, "AWS"),
        new CloudConfiguration(10, 2000, "t2.small", "us-west-2", 0.850, "AWS"),
        new CloudConfiguration(10, 4000, "t2.small", "us-west-2", 0.775, "AWS"),
        new CloudConfiguration(10, 8000, "t2.small", "us-west-2", 0.645, "AWS"),
        new CloudConfiguration(10, 16000, "t2.small", "us-west-2", 0.550, "AWS"),
        new CloudConfiguration(100, 1000, "t2.small", "us-west-2", 0.629, "AWS"),
        new CloudConfiguration(100, 2000, "t2.small", "us-west-2", 0.545, "AWS"),
        new CloudConfiguration(100, 4000, "t2.small", "us-west-2", 0.488, "AWS"),
        new CloudConfiguration(100, 8000, "t2.small", "us-west-2", 0.477, "AWS"),
        new CloudConfiguration(100, 16000, "t2.small", "us-west-2", 0.476, "AWS"),

        // AWS t2.medium, ap-southeast-1 (Singapore)
        new CloudConfiguration(10, 1000, "t2.medium", "ap-southeast-1", 0.950, "AWS"),
        new CloudConfiguration(10, 2000, "t2.medium", "ap-southeast-1", 0.810, "AWS"),
        new CloudConfiguration(10, 4000, "t2.medium", "ap-southeast-1", 0.910, "AWS"),
        new CloudConfiguration(10, 8000, "t2.medium", "ap-southeast-1", 0.710, "AWS"),
        new CloudConfiguration(10, 16000, "t2.medium", "ap-southeast-1", 0.420, "AWS"),
        new CloudConfiguration(100, 1000, "t2.medium", "ap-southeast-1", 0.644, "AWS"),
        new CloudConfiguration(100, 2000, "t2.medium", "ap-southeast-1", 0.396, "AWS"),
        new CloudConfiguration(100, 4000, "t2.medium", "ap-southeast-1", 0.125, "AWS"),
        new CloudConfiguration(100, 8000, "t2.medium", "ap-southeast-1", 0.035, "AWS"),
        new CloudConfiguration(100, 16000, "t2.medium", "ap-southeast-1", 0.009, "AWS"),

        // AWS t2.medium, us-west-2 (Oregon)
        new CloudConfiguration(10, 1000, "t2.medium", "us-west-2", 0.900, "AWS"),
        new CloudConfiguration(10, 2000, "t2.medium", "us-west-2", 0.850, "AWS"),
        new CloudConfiguration(10, 4000, "t2.medium", "us-west-2", 0.540, "AWS"),
        new CloudConfiguration(10, 8000, "t2.medium", "us-west-2", 0.450, "AWS"),
        new CloudConfiguration(10, 16000, "t2.medium", "us-west-2", 0.160, "AWS"),
        new CloudConfiguration(100, 1000, "t2.medium", "us-west-2", 0.319, "AWS"),
        new CloudConfiguration(100, 2000, "t2.medium", "us-west-2", 0.117, "AWS"),
        new CloudConfiguration(100, 4000, "t2.medium", "us-west-2", 0.024, "AWS"),
        new CloudConfiguration(100, 8000, "t2.medium", "us-west-2", 0.004, "AWS"),
        new CloudConfiguration(100, 16000, "t2.medium", "us-west-2", 0.002, "AWS"),

        // AWS t2.large, ap-southeast-1 (Singapore)
        new CloudConfiguration(10, 1000, "t2.large", "ap-southeast-1", 0.970, "AWS"),
        new CloudConfiguration(10, 2000, "t2.large", "ap-southeast-1", 0.990, "AWS"),
        new CloudConfiguration(10, 4000, "t2.large", "ap-southeast-1", 0.810, "AWS"),
        new CloudConfiguration(10, 8000, "t2.large", "ap-southeast-1", 0.500, "AWS"),
        new CloudConfiguration(10, 16000, "t2.large", "ap-southeast-1", 0.310, "AWS"),
        new CloudConfiguration(100, 1000, "t2.large", "ap-southeast-1", 0.529, "AWS"),
        new CloudConfiguration(100, 2000, "t2.large", "ap-southeast-1", 0.207, "AWS"),
        new CloudConfiguration(100, 4000, "t2.large", "ap-southeast-1", 0.023, "AWS"),
        new CloudConfiguration(100, 8000, "t2.large", "ap-southeast-1", 0.011, "AWS"),
        new CloudConfiguration(100, 16000, "t2.large", "ap-southeast-1", 0.003, "AWS"),

        // AWS t2.large, us-west-2 (Oregon)
        new CloudConfiguration(10, 1000, "t2.large", "us-west-2", 0.890, "AWS"),
        new CloudConfiguration(10, 2000, "t2.large", "us-west-2", 0.800, "AWS"),
        new CloudConfiguration(10, 4000, "t2.large", "us-west-2", 0.600, "AWS"),
        new CloudConfiguration(10, 8000, "t2.large", "us-west-2", 0.430, "AWS"),
        new CloudConfiguration(10, 16000, "t2.large", "us-west-2", 0.110, "AWS"),
        new CloudConfiguration(100, 1000, "t2.large", "us-west-2", 0.381, "AWS"),
        new CloudConfiguration(100, 2000, "t2.large", "us-west-2", 0.129, "AWS"),
        new CloudConfiguration(100, 4000, "t2.large", "us-west-2", 0.035, "AWS"),
        new CloudConfiguration(100, 8000, "t2.large", "us-west-2", 0.005, "AWS"),
        new CloudConfiguration(100, 16000, "t2.large", "us-west-2", 0.000, "AWS"),

        // GCP g1-small, asia-southeast1 (Singapore)
        new CloudConfiguration(10, 1000, "g1-small", "asia-southeast1", 0.940, "GCP"),
        new CloudConfiguration(10, 2000, "g1-small", "asia-southeast1", 0.871, "GCP"),
        new CloudConfiguration(10, 4000, "g1-small", "asia-southeast1", 0.667, "GCP"),
        new CloudConfiguration(10, 8000, "g1-small", "asia-southeast1", 0.512, "GCP"),
        new CloudConfiguration(10, 16000, "g1-small", "asia-southeast1", 0.251, "GCP"),
        new CloudConfiguration(100, 1000, "g1-small", "asia-southeast1", 0.510, "GCP"),
        new CloudConfiguration(100, 2000, "g1-small", "asia-southeast1", 0.260, "GCP"),
        new CloudConfiguration(100, 4000, "g1-small", "asia-southeast1", 0.091, "GCP"),
        new CloudConfiguration(100, 8000, "g1-small", "asia-southeast1", 0.078, "GCP"),
        new CloudConfiguration(100, 16000, "g1-small", "asia-southeast1", 0.019, "GCP"),

        // GCP g1-small, us-west1 (Oregon)
        new CloudConfiguration(10, 1000, "g1-small", "us-west1", 0.900, "GCP"),
        new CloudConfiguration(10, 2000, "g1-small", "us-west1", 0.820, "GCP"),
        new CloudConfiguration(10, 4000, "g1-small", "us-west1", 0.520, "GCP"),
        new CloudConfiguration(10, 8000, "g1-small", "us-west1", 0.420, "GCP"),
        new CloudConfiguration(10, 16000, "g1-small", "us-west1", 0.250, "GCP"),
        new CloudConfiguration(100, 1000, "g1-small", "us-west1", 0.411, "GCP"),
        new CloudConfiguration(100, 2000, "g1-small", "us-west1", 0.235, "GCP"),
        new CloudConfiguration(100, 4000, "g1-small", "us-west1", 0.076, "GCP"),
        new CloudConfiguration(100, 8000, "g1-small", "us-west1", 0.059, "GCP"),
        new CloudConfiguration(100, 16000, "g1-small", "us-west1", 0.014, "GCP"),

        // GCP n1-standard-1, asia-southeast1 (Singapore)
        new CloudConfiguration(10, 1000, "n1-standard-1", "asia-southeast1", 1.000, "GCP"),
        new CloudConfiguration(10, 2000, "n1-standard-1", "asia-southeast1", 0.970, "GCP"),
        new CloudConfiguration(10, 4000, "n1-standard-1", "asia-southeast1", 0.885, "GCP"),
        new CloudConfiguration(10, 8000, "n1-standard-1", "asia-southeast1", 0.480, "GCP"),
        new CloudConfiguration(10, 16000, "n1-standard-1", "asia-southeast1", 0.0439, "GCP"),
        new CloudConfiguration(100, 1000, "n1-standard-1", "asia-southeast1", 0.180, "GCP"),
        new CloudConfiguration(100, 2000, "n1-standard-1", "asia-southeast1", 0.0467, "GCP"),
        new CloudConfiguration(100, 4000, "n1-standard-1", "asia-southeast1", 0.0167, "GCP"),
        new CloudConfiguration(100, 8000, "n1-standard-1", "asia-southeast1", 0.0083, "GCP"),
        new CloudConfiguration(100, 16000, "n1-standard-1", "asia-southeast1", 0.000, "GCP"),

        // GCP n1-standard-1, us-west1 (Oregon)
        new CloudConfiguration(10, 1000, "n1-standard-1", "us-west1", 0.881, "GCP"),
        new CloudConfiguration(10, 2000, "n1-standard-1", "us-west1", 0.784, "GCP"),
        new CloudConfiguration(10, 4000, "n1-standard-1", "us-west1", 0.700, "GCP"),
        new CloudConfiguration(10, 8000, "n1-standard-1", "us-west1", 0.489, "GCP"),
        new CloudConfiguration(10, 16000, "n1-standard-1", "us-west1", 0.100, "GCP"),
        new CloudConfiguration(100, 1000, "n1-standard-1", "us-west1", 0.520, "GCP"),
        new CloudConfiguration(100, 2000, "n1-standard-1", "us-west1", 0.329, "GCP"),
        new CloudConfiguration(100, 4000, "n1-standard-1", "us-west1", 0.092, "GCP"),
        new CloudConfiguration(100, 8000, "n1-standard-1", "us-west1", 0.028, "GCP"),
        new CloudConfiguration(100, 16000, "n1-standard-1", "us-west1", 0.013, "GCP"),

        // GCP n1-standard-2, asia-southeast1 (Singapore)
        new CloudConfiguration(10, 1000, "n1-standard-2", "asia-southeast1", 0.93, "GCP"),
        new CloudConfiguration(10, 2000, "n1-standard-2", "asia-southeast1", 0.91, "GCP"),
        new CloudConfiguration(10, 4000, "n1-standard-2", "asia-southeast1", 0.72, "GCP"),
        new CloudConfiguration(10, 8000, "n1-standard-2", "asia-southeast1", 0.51, "GCP"),
        new CloudConfiguration(10, 16000, "n1-standard-2", "asia-southeast1", 0.32, "GCP"),
        new CloudConfiguration(100, 1000, "n1-standard-2", "asia-southeast1", 0.469, "GCP"),
        new CloudConfiguration(100, 2000, "n1-standard-2", "asia-southeast1", 0.206, "GCP"),
        new CloudConfiguration(100, 4000, "n1-standard-2", "asia-southeast1", 0.194, "GCP"),
        new CloudConfiguration(100, 8000, "n1-standard-2", "asia-southeast1", 0.086, "GCP"),
        new CloudConfiguration(100, 16000, "n1-standard-2", "asia-southeast1", 0.0034, "GCP"),

        // GCP n1-standard-2, us-west1 (Oregon)
        new CloudConfiguration(10, 1000, "n1-standard-2", "us-west1", 0.88, "GCP"),
        new CloudConfiguration(10, 2000, "n1-standard-2", "us-west1", 0.81, "GCP"),
        new CloudConfiguration(10, 4000, "n1-standard-2", "us-west1", 0.69, "GCP"),
        new CloudConfiguration(10, 8000, "n1-standard-2", "us-west1", 0.48, "GCP"),
        new CloudConfiguration(10, 16000, "n1-standard-2", "us-west1", 0.29, "GCP"),
        new CloudConfiguration(100, 1000, "n1-standard-2", "us-west1", 0.41, "GCP"),
        new CloudConfiguration(100, 2000, "n1-standard-2", "us-west1", 0.19, "GCP"),
        new CloudConfiguration(100, 4000, "n1-standard-2", "us-west1", 0.17, "GCP"),
        new CloudConfiguration(100, 8000, "n1-standard-2", "us-west1", 0.073, "GCP"),
        new CloudConfiguration(100, 16000, "n1-standard-2", "us-west1", 0.0017, "GCP")
    );
    
    /**
     * Calculate squared Euclidean distance between two workload points
     * @param tps1 TPS of first workload
     * @param tx1 Transactions of first workload  
     * @param tps2 TPS of second workload
     * @param tx2 Transactions of second workload
     * @return squared distance
     */
    private static long calculateSquaredDistance(int tps1, int tx1, int tps2, int tx2) {
        long dtps = tps1 - tps2;
        long dtx = tx1 - tx2;
        return dtps * dtps + dtx * dtx;
    }
    
    /**
     * Find top 3 nearest neighbors with Apdex filtering
     * @param targetTPS Target workload TPS
     * @param targetTransactions Target workload transactions
     * @param minApdex Minimum Apdex score filter
     * @return List of top 3 KNN results sorted by distance
     */
    public static List<KNNResult> getInstanceKnn3(int targetTPS, int targetTransactions, double minApdex) {
        // Filter configurations by Apdex threshold and calculate distances
        List<KNNResult> candidates = CLOUD_DATA.stream()
            .filter(config -> config.apdex >= minApdex)
            .map(config -> {
                long distance = calculateSquaredDistance(targetTPS, targetTransactions, config.tps, config.transactions);
                return new KNNResult(config, distance);
            })
            .sorted(Comparator.comparing(result -> result.distance))
            .limit(3)
            .collect(Collectors.toList());
            
        return candidates;
    }
    
    /**
     * Main method - command line interface
     * Usage: java NFRKnn3 <TPS> <Transactions> [Min_Apdex]
     */
    // public static void main(String[] args) {
    //     if (args.length < 2) {
    //         System.err.println("Usage: java NFRKnn3 <TPS> <Transactions> [Min_Apdex]");
    //         System.err.println("Example: java NFRKnn3 10 3000");
    //         System.err.println("Example: java NFRKnn3 10 3000 0.90");
    //         System.exit(1);
    //     }
        
    //     try {
    //         int tps = Integer.parseInt(args[0]);
    //         int transactions = Integer.parseInt(args[1]);
    //         double minApdex = args.length >= 3 ? Double.parseDouble(args[2]) : 0.0;
            
    //         System.out.printf("Top 3 recommendations for workload (%d TPS, %d transactions) with Apdex ≥ %.2g:%n", 
    //                         tps, transactions, minApdex);
            
    //         List<KNNResult> results = getInstanceKnn3(tps, transactions, minApdex);
            
    //         if (results.isEmpty()) {
    //             System.out.printf("⚠️ No results meet Apdex ≥ %.2g%n", minApdex);
    //         } else {
    //             for (KNNResult result : results) {
    //                 System.out.println(result);
    //             }
    //         }
            
    //     } catch (NumberFormatException e) {
    //         System.err.println("Error: Invalid number format in arguments");
    //         System.err.println("Usage: java NFRKnn3 <TPS> <Transactions> [Min_Apdex]");
    //         System.exit(1);
    //     }
    // }
}