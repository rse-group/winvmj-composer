<?xml version="1.0" encoding="UTF-8"?>
<aspectj>
    <aspects>
        <!-- MonitoringAspect for method call instrumentation -->
        <aspect name="${aspectPackage}.MonitoringAspect"/>
    </aspects>
    
    <weaver options="-verbose -showWeaveInfo">
        <!-- Include the aspect itself for weaving (generates aspectOf()) -->
        <include within="${aspectPackage}..*"/>
        <!-- Include packages to be woven -->
        <#list monitoredPackages as pkg>
        <include within="${pkg}..*"/>
        </#list>
<#if anyDbMetricsEnabled>
        <!-- Include Hibernate JDBC internals for DB metrics -->
        <include within="org.hibernate.engine.jdbc.internal..*"/>
</#if>
        
        <!-- Exclude AspectJ and OpenTelemetry internals -->
        <exclude within="org.aspectj..*"/>
        <exclude within="io.opentelemetry..*"/>
    </weaver>
</aspectj>
