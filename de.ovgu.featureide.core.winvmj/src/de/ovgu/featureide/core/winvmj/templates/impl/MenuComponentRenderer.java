package de.ovgu.featureide.core.winvmj.templates.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import de.ovgu.featureide.core.IFeatureProject;
import de.ovgu.featureide.core.winvmj.core.WinVMJProduct;
import de.ovgu.featureide.core.winvmj.runtime.WinVMJConsole;
import de.ovgu.featureide.core.winvmj.templates.ReactTemplateRenderer;
import de.ovgu.featureide.core.winvmj.templates.TemplateRenderer;
import freemarker.template.Template;

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
