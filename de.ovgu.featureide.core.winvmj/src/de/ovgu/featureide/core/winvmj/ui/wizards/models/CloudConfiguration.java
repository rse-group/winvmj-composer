package de.ovgu.featureide.core.winvmj.ui.wizards.misc.model;

/**
 * Represents a cloud configuration with performance metrics (AWS or GCP)
*/
public class CloudConfiguration {
    public final int tps;
    public final int transactions;
    public final String instanceType;
    public final String region;
    public final double apdex;
    public final String provider;
    
    public CloudConfiguration(int tps, int transactions, String instanceType, String region, double apdex, String provider) {
        this.tps = tps;
        this.transactions = transactions;
        this.instanceType = instanceType;
        this.region = region;
        this.apdex = apdex;
        this.provider = provider;
    }
    
    // Backward compatibility constructor for existing code
    public CloudConfiguration(int tps, int transactions, String instanceType, String region, double apdex, String provider) {
        this(tps, transactions, instanceType, region, apdex, provider);
    }
    
    @Override
    public String toString() {
        return String.format("%s | Instance=%s | Region=%s | Workload=(%d TPS, %d tx) | Apdex=%.2f", 
                            provider, instanceType, region, tps, transactions, apdex);
    }
}