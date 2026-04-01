monitoring.enabled=${monitoringEnabled?c}
monitoring.features=<#list monitoredFeatures as f>${f}<#if f_has_next>,</#if></#list>
