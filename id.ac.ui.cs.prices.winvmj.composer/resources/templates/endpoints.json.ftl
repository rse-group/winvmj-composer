{
    "excludes": [
    <#list excludedEndpoints as excludedEndpoint>
        "${excludedEndpoint}"<#if excludedEndpoint?has_next>,</#if>
    </#list>
    ]
}
