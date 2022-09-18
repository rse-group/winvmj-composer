package de.ovgu.featureide.core.winvmj.templates.impl;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import de.ovgu.featureide.core.IFeatureProject;
import de.ovgu.featureide.core.winvmj.core.WinVMJProduct;
import de.ovgu.featureide.core.winvmj.runtime.WinVMJConsole;
import de.ovgu.featureide.core.winvmj.templates.TemplateRenderer;

public class MenuComponentRenderer extends TemplateRenderer {
	
	String[] selectedFeature;
	Map<String, Object> featureMap;
	
	public MenuComponentRenderer(IFeatureProject project,
			String[] selectedFeature2, Map<String, Object> featureMap) {
		super(project);
		this.selectedFeature = selectedFeature2;
		this.featureMap = featureMap;
	}
	
	protected Map<String, Object> extractDataModel(WinVMJProduct product) {
		Map<String, Object> dataModel = new HashMap<>();
		Map<String, Object>[] selectedMap = new Map[selectedFeature.length];
		
		for (int featureIndex = 0; featureIndex < selectedFeature.length; featureIndex++) {
//		for (Map<String, Object> f : featureMap) {
//			Map<String, Object> mapping = new HashMap<>();
//			NamedNodeMap attributes = selectedFeature[featureIndex].getAttributes();
//			int attributeLength = attributes.getLength();
//			for (int i = 0; i < attributeLength; i++) {
//				Node attribute = attributes.item(i);
//				mapping.put(attribute.getNodeName(), attribute.getNodeValue());
//			}
			String featureName = selectedFeature[featureIndex];
//			WinVMJConsole.println(feature.getNodeName());
			WinVMJConsole.println("Here: " + featureIndex);
			WinVMJConsole.println("Here: " + featureName);
			selectedMap[featureIndex] = (Map<String, Object>) featureMap.get(featureName);
		}
		
		dataModel.put("features", selectedMap);
		
		return dataModel;
	}
	
	protected String loadTemplateFilename() {
		return "MenuComponent";
	}
	
	protected IFile getOutputFile(WinVMJProduct product) {
		return project.getProject().getFolder("routing")
				.getFile("main-menu-component.js");
	}
}
