module ${monitoringModuleName} {
    exports ${monitoringModuleName};
    
    requires java.logging;
    requires io.opentelemetry.api;
    requires io.opentelemetry.sdk;
    requires io.opentelemetry.sdk.common;
    requires io.opentelemetry.sdk.metrics;
    requires io.opentelemetry.sdk.logs;
    requires io.opentelemetry.exporter.otlp;
    requires io.opentelemetry.exporter.otlp.common;
    requires io.opentelemetry.exporter.common;
<#if anyTracingEnabled>
    requires io.opentelemetry.sdk.trace;
</#if>
<#if enableJvmMetrics>
    requires io.opentelemetry.instrumentation.runtime_telemetry_java8;
</#if>
    requires org.aspectj.weaver;
    
    opens ${monitoringModuleName} to org.aspectj.weaver;
}
