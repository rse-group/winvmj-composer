package id.ac.ui.cs.prices.winvmj.composer.templates.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;

import de.ovgu.featureide.core.IFeatureProject;
import id.ac.ui.cs.prices.winvmj.composer.core.WinVMJProduct;
import id.ac.ui.cs.prices.winvmj.composer.templates.TemplateRenderer;
import id.ac.ui.cs.prices.winvmj.composer.Utils;

public class ModuleInfoRenderer extends TemplateRenderer {

	protected static List<String> exportedModules;
	protected Map<String, List<String>> multiLevelDeltaMappings;
	private Map<String, Map<String, Object>> monitoringConfig;
	protected List<String> selectedFeature;
	
	public ModuleInfoRenderer(IFeatureProject project) {
		super(project);
		exportedModules = new ArrayList<>();
		getSelectedFeature(project);
		try {
			monitoringConfig = Utils.getFeatureMonitoringConfig(project.getProject());
		} catch (Exception e) {
			monitoringConfig = new HashMap<>();
			System.err.println("Failed to load monitoring config: " + e.getMessage());
		}
	}

	public ModuleInfoRenderer(
		IFeatureProject project,
		Map<String, List<String>> multiLevelDeltaMappings
	) {
		super(project);
		exportedModules = new ArrayList<>();
		this.multiLevelDeltaMappings = multiLevelDeltaMappings;
		getSelectedFeature(project);
		try {
			monitoringConfig = Utils.getFeatureMonitoringConfig(project.getProject());
		} catch (Exception e) {
			monitoringConfig = new HashMap<>();
			System.err.println("Failed to load monitoring config: " + e.getMessage());
		}
	}
	
	protected Map<String, Object> extractDataModel(WinVMJProduct product) {
		Map<String, Object> dataModel = new HashMap<>();
		
		dataModel.put("productPackage", product.getProductQualifiedName());
		dataModel.put("requiredModules", getRequiredModules(product));
		dataModel.put("exportedModules", exportedModules);
		
		// Add monitoring flag for OpenTelemetry modules
		boolean monitoringEnabled = isAnyFeatureMonitoringEnabled(product);
		dataModel.put("monitoringEnabled", monitoringEnabled);
		
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
	
	private boolean isAnyFeatureMonitoringEnabled(WinVMJProduct product) {
		if (monitoringConfig == null || monitoringConfig.isEmpty()) {
			return false;
		}
		for (String feature : selectedFeature) {
			if (Utils.isFeatureMonitoringEnabled(monitoringConfig, feature)) {
				return true;
			}
		}
		return false;
	}
	
	private void getSelectedFeature(IFeatureProject winVmjProject) {
		try {
			Set<String> features = winVmjProject.loadCurrentConfiguration().getSelectedFeatureNames();
			// Remove module prefix (e.g., "aisco.Program" -> "Program")
			features = features.stream()
				.map(name -> name.contains(".") ? name.substring(name.indexOf('.') + 1) : name)
				.collect(Collectors.toSet());
			selectedFeature = new ArrayList<>(features);
		} catch (Exception e) {
			System.err.println("Failed to load selected features: " + e.getMessage());
			selectedFeature = new ArrayList<>();
		}
	}
}
