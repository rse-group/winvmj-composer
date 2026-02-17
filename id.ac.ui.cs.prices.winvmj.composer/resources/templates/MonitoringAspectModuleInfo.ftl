module ${modulePackage} {
    exports ${modulePackage}.monitoring;
    
    requires java.logging;
    requires io.opentelemetry.api;
    requires io.opentelemetry.sdk;
    requires io.opentelemetry.sdk.metrics;
    requires io.opentelemetry.exporter.prometheus;
<#if enableJvmMetrics>
    requires io.opentelemetry.instrumentation.runtime_telemetry_java8;
</#if>
    
    opens ${modulePackage}.monitoring to org.aspectj.weaver;
}
