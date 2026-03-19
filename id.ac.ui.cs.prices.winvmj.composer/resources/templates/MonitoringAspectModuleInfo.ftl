module ${monitoringModuleName} {
    exports ${monitoringModuleName};
    
    requires java.logging;
    requires org.slf4j;
    requires id.ac.ui.cs.prices.winvmj.core;
    requires io.opentelemetry.api;
    requires io.opentelemetry.sdk;
    requires io.opentelemetry.sdk.common;
    requires io.opentelemetry.sdk.metrics;
    requires io.opentelemetry.exporter.otlp;
<#if anyTracingEnabled>
    requires io.opentelemetry.sdk.trace;
</#if>
<#if anyLoggingEnabled>
    requires io.opentelemetry.sdk.logs;
    requires ch.qos.logback.classic;
    requires ch.qos.logback.core;
    requires io.opentelemetry.instrumentation.logback_appender_1_0;
</#if>
<#if enableJvmMetrics>
    requires io.opentelemetry.instrumentation.runtime_telemetry_java8;
</#if>
    requires org.aspectj.weaver;
    
    opens ${monitoringModuleName} to org.aspectj.weaver;
}
