package id.ac.ui.cs.prices.winvmj.composer.templates.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

import org.eclipse.core.runtime.CoreException;

import de.ovgu.featureide.core.IFeatureProject;
import id.ac.ui.cs.prices.winvmj.composer.Utils;
import id.ac.ui.cs.prices.winvmj.composer.core.WinVMJProduct;

public class MicroserviceProductClassRenderer extends ProductClassRenderer {
		
	public MicroserviceProductClassRenderer(IFeatureProject project,
			List<String> routingFeatures) {
		super(project);
		this.selectedFeatures = new HashSet<>(routingFeatures);
	}
	
	@Override
	protected Set<String> getImports(WinVMJProduct product) throws IOException, CoreException {
		Map<String, List<String>> featureToModuleNameMap = Utils.getFeatureToModuleMap(project.getProject());
		List<String> selectedFeatureModulesName = new ArrayList<String>();
		
		for (String feature : selectedFeatures) {
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