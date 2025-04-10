package de.ovgu.featureide.core.winvmj.templates.impl;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;

import de.ovgu.featureide.core.winvmj.templates.ReactTemplateRenderer;

public class RoutingComponentRenderer extends ReactTemplateRenderer {
	
	public RoutingComponentRenderer(String[] selectedFeature, Map<String, Object> featureMap) {
		super(selectedFeature, featureMap);
	}

	protected Map<String, Object> extractDataModel() {
		Map<String, Object> dataModel = new HashMap();
		Map<String, Object> selectedMap = new HashMap();
		
		for (int featureIndex = 0; featureIndex < selectedFeature.length; featureIndex++) {
			String featureName = selectedFeature[featureIndex];
			Map<String, Object> featureRouting = (Map<String, Object>) featureMap.get(featureName);
			String routeName = (String) featureRouting.get("routename");
			String routeFilePath = (String) featureRouting.get("routefilepath");
			if (!selectedMap.containsKey(routeName)) {
				selectedMap.put(routeName, routeFilePath);
			}
		}
		
		dataModel.put("features", selectedMap);
		
		return dataModel;
	}
	
	private Map<String, Object> createDefaultMap(String featurename) {
		return null;
	}
	
	protected String loadTemplateFilename() {
		return "RoutingComponent";
	}
	
	protected IFile getOutputFile(IProject targetProject) {
		return targetProject.getFolder("src")
				.getFile("routes.js");
	}
}
