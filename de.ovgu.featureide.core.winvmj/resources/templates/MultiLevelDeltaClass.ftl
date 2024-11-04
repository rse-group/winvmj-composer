package ${featurePackage};

import java.util.*;

import ${splName}.core.${feature};
import ${splName}.core.${feature}${businessLayer};
import ${splName}.core.${feature}${businessLayer}Decorator;
import ${splName}.core.${feature}${businessLayer}Component;
import ${splName}.core.${feature}${businessLayer}Factory;

public class ${feature}${businessLayer}Impl extends ${feature}${businessLayer}Decorator {
    private static ${feature}${businessLayer} RESOURCE;
    private final String[] DELTA_MODULES = {
        <#list deltas as delta>
		<#if delta?index != (deltas?size - 1)>
		${delta},
		<#else>
		${delta}
		</#if>
		</#list>
	};

    public ${feature}${businessLayer}Impl(${feature}${businessLayer} record) {
		RESOURCE = ${feature}${businessLayer}Factory.create${feature}${businessLayer}(
			"${baseComponent}");

		for (int i = ${initialDeltaIndex}; i < DELTA_MODULES.length; i++) {
			RESOURCE = ${feature}${businessLayer}Factory.create${feature}${businessLayer}(
				DELTA_MODULES[i],
				RESOURCE
			);
		}

		record = (${feature}${businessLayer}Component) RESOURCE;
	}
}