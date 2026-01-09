package id.ac.ui.cs.prices.winvmj.composer.ui.wizards.models;

import id.ac.ui.cs.prices.winvmj.composer.ui.wizards.models.CloudConfiguration;

/**
 * Represents a candidate result with distance information
 */
public class KNNResult {
    public final CloudConfiguration config;
    public final long distance;
    
    public KNNResult(CloudConfiguration config, long distance) {
        this.config = config;
        this.distance = distance;
    }

    public CloudConfiguration getConfig() {
        return this.config;
    }
    
    @Override
    public String toString() {
        return String.format("%s | Instance=%s | Region=%s | Workload=(%d TPS, %d tx) | Apdex=%.3g | Dist=%d", 
                            config.provider, config.instanceType, config.region, config.tps, config.transactions, config.apdex, distance);
    }
}
