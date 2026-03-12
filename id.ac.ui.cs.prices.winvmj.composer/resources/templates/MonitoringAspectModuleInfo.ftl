module ${monitoringModuleName} {
    exports ${monitoringModuleName};
    
    requires java.logging;
    requires io.opentelemetry.api;
    requires io.opentelemetry.sdk;
    requires io.opentelemetry.sdk.metrics;
    requires io.opentelemetry.exporter.prometheus;
<#if enableJvmMetrics>
    requires io.opentelemetry.instrumentation.runtime_telemetry_java8;
</#if>
    
    opens ${monitoringModuleName} to org.aspectj.weaver;
}
