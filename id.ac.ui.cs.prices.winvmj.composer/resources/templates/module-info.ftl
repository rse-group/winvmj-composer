module ${productPackage} {
    requires id.ac.ui.cs.prices.winvmj.auth;
    requires id.ac.ui.cs.prices.winvmj.auth.model;
    requires id.ac.ui.cs.prices.winvmj.core;
    requires id.ac.ui.cs.prices.winvmj.hibernate;
    requires org.slf4j;
    
    requires net.bytebuddy;
    requires java.xml.bind;
    requires com.sun.xml.bind;
    requires com.fasterxml.classmate;
    requires jdk.unsupported;

    <#list requiredModules as requiredModule>
    requires ${requiredModule};
    </#list>

    <#if monitoringEnabled!false && monitoringMode!"" == "DOP">
    requires io.opentelemetry.api;
    requires io.opentelemetry.sdk;
    requires io.opentelemetry.sdk.metrics;
    requires io.opentelemetry.sdk.common;
    requires io.opentelemetry.exporter.otlp;
    <#if anyTracingEnabled!false>
    requires io.opentelemetry.sdk.trace;
    </#if>
    <#if anyLoggingEnabled!false>
    requires io.opentelemetry.sdk.logs;
    requires io.opentelemetry.logback.appender;
    </#if>
    <#if enableJvmMetrics!false>
    requires io.opentelemetry.instrumentation.runtimemetrics.java8;
    </#if>
    </#if>

    <#list exportedModules as exportedModule>
    exports ${exportedModule};
    </#list>
}