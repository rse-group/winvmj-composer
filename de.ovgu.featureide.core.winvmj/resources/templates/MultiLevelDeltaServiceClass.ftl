package ${package};

import java.util.*;

import ${coreModule}.${featureName};
import ${coreModule}.${featureName}Service;
import ${coreModule}.${featureName}ServiceDecorator;
import ${coreModule}.${featureName}ServiceComponent;
import ${splName}.${loweredFeatureName}.${featureName}ServiceFactory;

public class ${featureName}ServiceImpl extends ${featureName}ServiceDecorator {
    private static ${featureName}Service RESOURCE;
    private final String[] DELTA_MODULES = {
        <#list deltas as delta>
		<#if delta?index != (deltas?size - 1)>
		"${delta}",
		<#else>
		"${delta}"
		</#if>
		</#list>
	};

    public ${featureName}ServiceImpl(${featureName}ServiceComponent record) {
		super(record);
		RESOURCE = ${featureName}ServiceFactory.create${featureName}Service(
			"${baseComponent}");

		for (int i = ${initialDeltaIndex}; i < DELTA_MODULES.length; i++) {
			RESOURCE = ${featureName}ServiceFactory.create${featureName}Service(
				DELTA_MODULES[i],
				RESOURCE
			);
		}

		record = (${featureName}ServiceComponent) RESOURCE;
	}

	<#list methods as method>
	${method['signature']} {
		<#if method['returnType'] == 'void'>
		RESOURCE.${method['methodName']}(${method['parameterNames']});
		<#else>
		return RESOURCE.${method['methodName']}(${method['parameterNames']});
		</#if>
	}
	<#if method?index != (methods?size - 1)>

	</#if>
	</#list>
}