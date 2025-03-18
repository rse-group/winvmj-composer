package de.ovgu.featureide.core.winvmj.templates.impl;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IFile;

import de.ovgu.featureide.core.IFeatureProject;
import de.ovgu.featureide.core.winvmj.core.WinVMJProduct;
import de.ovgu.featureide.core.winvmj.templates.TemplateRenderer;

public class CorsPropertiesRenderer extends TemplateRenderer {
	
	
	public CorsPropertiesRenderer(IFeatureProject project) {
		super(project);
	}
	
	protected Map<String, Object> extractDataModel(WinVMJProduct product) {
		Map<String, Object> dataModel = new HashMap<>();
		
		dataModel.put("allowedMethod", "GET, POST, PUT, DELETE, PATCH");
		dataModel.put("allowedOrigin", "*");
		
		return dataModel;
	}
	
	protected String loadTemplateFilename() {
		return "cors.properties";
	}
	
	protected IFile getOutputFile(WinVMJProduct product) {
		return project.getProject().getFolder("src-gen")
				.getFolder(product.getProductName())
				.getFile("cors.properties");
	}
}
