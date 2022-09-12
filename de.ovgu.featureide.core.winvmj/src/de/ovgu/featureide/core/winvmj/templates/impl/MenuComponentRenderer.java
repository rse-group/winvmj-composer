package de.ovgu.featureide.core.winvmj.templates.impl;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.w3c.dom.Element;

import de.ovgu.featureide.core.IFeatureProject;
import de.ovgu.featureide.core.winvmj.core.WinVMJProduct;
import de.ovgu.featureide.core.winvmj.templates.TemplateRenderer;

public class MenuComponentRenderer extends TemplateRenderer {
	
	Element[] selectedFeature;
	
	public MenuComponentRenderer(IFeatureProject project, Element[] selectedFeature) {
		super(project);
		this.selectedFeature = selectedFeature;
	}
	
	protected Map<String, Object> extractDataModel(WinVMJProduct product) {
		Map<String, Object> dataModel = new HashMap<>();
		
		dataModel.put("dbname", "value-dbname");
		dataModel.put("product", "value-product");
		dataModel.put("dbUser", "value-username");
		dataModel.put("dbPassword", "value-password");
		
		for (Element element : selectedFeature) {
			dataModel.put(element.getAttribute("name").toLowerCase(), true);
		}
		
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
