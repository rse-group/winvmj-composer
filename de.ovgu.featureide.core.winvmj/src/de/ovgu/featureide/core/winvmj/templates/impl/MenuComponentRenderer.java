package de.ovgu.featureide.core.winvmj.templates.impl;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;

import de.ovgu.featureide.core.winvmj.templates.ReactTemplateRenderer;

public class MenuComponentRenderer extends ReactTemplateRenderer {
	
	private Map<String, Object>[] modelStructureMap;

	public MenuComponentRenderer(String[] selectedFeature, Map<String, Object> featureMap, Map<String, Object>[] modelStructureMap) {
		super(selectedFeature, featureMap);
		this.modelStructureMap = modelStructureMap;
	}

	protected Map<String, Object> extractDataModel() {
		Map<String, Object> dataModel = new HashMap<>();
		
		dataModel.put("features", selectedFeature);
		dataModel.put("structures", modelStructureMap);
		
		return dataModel;
	}
	
	protected String loadTemplateFilename() {
		return "MenuComponent";
	}
	
	protected IFile getOutputFile(IProject targetProject) {
		return targetProject.getFolder("src")
				.getFile("menus.js");
	}
}
