package de.ovgu.featureide.core.winvmj.templates.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;

import de.ovgu.featureide.core.IFeatureProject;
import de.ovgu.featureide.core.winvmj.Utils;
import de.ovgu.featureide.core.winvmj.core.WinVMJProduct;

public class MicroserviceProductClassRenderer extends ProductClassRenderer {
		
	public MicroserviceProductClassRenderer(IFeatureProject project,
			List<String> routingFeatures) {
		super(project);
		this.selectedFeature = routingFeatures;
	}
	
	@Override
	protected Set<String> getImports(WinVMJProduct product) throws IOException, CoreException {
		Map<String, List<String>> featureToModuleNameMap = Utils.getFeatureToModuleMap(project.getProject());
		List<String> selectedFeatureModulesName = new ArrayList<String>();
		
		for (String feature : selectedFeature) {
    		List<String> featureModulesName = featureToModuleNameMap.getOrDefault(feature, null);
    		if (featureModulesName != null) {
    			for (String moduleName : featureModulesName) {
    				selectedFeatureModulesName.add(moduleName);
    			}
    		}
    	}
		
		Set<String> imports = new LinkedHashSet<>();
		for (String module : selectedFeatureModulesName) {
			imports.addAll(constructImport(module));
		}
		return imports;
	}
}