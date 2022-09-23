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

public class RoutingComponentRenderer extends ReactTemplateRenderer {
	
	public RoutingComponentRenderer(String[] selectedFeature, Map<String, Object> featureMap) {
		super(selectedFeature, featureMap);
	}

	protected Map<String, Object> extractDataModel() {
		Map<String, Object> dataModel = new HashMap<>();
		Map<String, Object>[] selectedMap = new Map[selectedFeature.length];
		
		for (int featureIndex = 0; featureIndex < selectedFeature.length; featureIndex++) {
			String featureName = selectedFeature[featureIndex];
			selectedMap[featureIndex] = (Map<String, Object>) featureMap.get(featureName);
		}
		
		dataModel.put("features", selectedMap);
		
		return dataModel;
	}
	
	protected String loadTemplateFilename() {
		return "RoutingComponent";
	}
	
	protected IFile getOutputFile(IProject targetProject) {
		return targetProject.getFolder("src")
				.getFile("routes.js");
	}
}
