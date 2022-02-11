module ${productPackage} {
    requires vmj.object.mapper;
    requires vmj.routing.route;

    <#list requiredModules as module>
    requires ${isReusable?then('transitive ', '')}${module};
    </#list>
    
    <#list externalModules as module>
    requires ${isReusable?then('transitive ', '')}${module};
    </#list>
	
	requires prices.auth.vmj;
    requires prices.auth.vmj.model;
}