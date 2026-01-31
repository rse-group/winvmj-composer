module ${modulePackage} {
    // OpenTelemetry modules for monitoring
    requires io.opentelemetry.api;
    requires io.opentelemetry.sdk;
    requires io.opentelemetry.sdk.metrics;
    requires io.opentelemetry.exporter.prometheus;
    
    // AspectJ for monitoring aspects
    requires org.aspectj.runtime;
    requires org.aspectj.weaver;
    
    // Export the monitoring package
    exports ${modulePackage}.monitoring;
}
