module ${productPackage} {
    requires id.ac.ui.cs.prices.winvmj.auth;
    requires id.ac.ui.cs.prices.winvmj.auth.model;
    requires id.ac.ui.cs.prices.winvmj.core;
    requires id.ac.ui.cs.prices.winvmj.hibernate;
    
    requires net.bytebuddy;
    requires java.xml.bind;
    requires com.sun.xml.bind;
    requires com.fasterxml.classmate;
    requires jdk.unsupported;

<#if monitoringEnabled>
    // OpenTelemetry modules for monitoring
    requires io.opentelemetry.api;
    requires io.opentelemetry.sdk;
    requires io.opentelemetry.sdk.metrics;
    requires io.opentelemetry.exporter.prometheus;
    
    // AspectJ for monitoring aspects
    requires org.aspectj.runtime;
    requires org.aspectj.weaver;
</#if>

    <#list requiredModules as requiredModule>
    requires ${requiredModule};
    </#list>

    <#list exportedModules as exportedModule>
    exports ${exportedModule};
    </#list>
}