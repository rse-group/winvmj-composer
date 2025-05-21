package de.ovgu.featureide.core.winvmj.templates.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.List;
import java.util.ArrayList;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;

import de.ovgu.featureide.core.IFeatureProject;
import de.ovgu.featureide.core.winvmj.core.WinVMJProduct;
import de.ovgu.featureide.core.winvmj.templates.TemplateRenderer;

public class ModuleInfoRenderer extends TemplateRenderer {

	protected static List<String> exportedModules;
	protected Map<String, List<String>> multiLevelDeltaMappings;
	
	public ModuleInfoRenderer(IFeatureProject project) {
		super(project);
		exportedModules = new ArrayList<>();
	}

	public ModuleInfoRenderer(
		IFeatureProject project,
		Map<String, List<String>> multiLevelDeltaMappings
	) {
		super(project);
		exportedModules = new ArrayList<>();
		this.multiLevelDeltaMappings = multiLevelDeltaMappings;
	}
	
	protected Map<String, Object> extractDataModel(WinVMJProduct product) {
		Map<String, Object> dataModel = new HashMap<>();
		
		dataModel.put("productPackage", product.getProductQualifiedName());
		dataModel.put("requiredModules", getRequiredModules(product));
		dataModel.put("exportedModules", exportedModules);
		
		return dataModel;
	}

	
	protected String loadTemplateFilename() {
		return "module-info";
	}
	
	protected IFile getOutputFile(WinVMJProduct product) {
		return project.getBuildFolder().getFolder(product.getProductQualifiedName()).getFile("module-info.java");
	}

	private List<String> getRequiredModules(WinVMJProduct product) {
		if (multiLevelDeltaMappings == null) {
			return product.getModuleNames();
		} 
		
		List<String> requiredModules = new ArrayList<>();
		requiredModules.addAll(product.getModuleNames());
		for (Entry<String, List<String>> mapping: multiLevelDeltaMappings.entrySet()) {
			String firstDeltaModule = mapping.getValue().get(0);
			String[] splittedFirstDeltaModule = firstDeltaModule.split("\\.");
			String splName = splittedFirstDeltaModule[0];
			String featureName = splittedFirstDeltaModule[1];
			String multiLevelDeltaModule = String.format(
				"%s.%s.%s", 
				splName,
				featureName,
				mapping.getKey().toLowerCase()
			);

			IFolder moduleFolder = project.getBuildFolder()
				.getFolder(multiLevelDeltaModule + featureName);
			if (moduleFolder.exists()) multiLevelDeltaModule += featureName;

			requiredModules.add(multiLevelDeltaModule);
		}

		return requiredModules;
	}
}
