package de.ovgu.featureide.core.winvmj.templates.impl;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IFile;

import de.ovgu.featureide.core.IFeatureProject;
import de.ovgu.featureide.core.winvmj.core.WinVMJProduct;
import de.ovgu.featureide.core.winvmj.templates.TemplateRenderer;

public class ModuleInfoHibernateRenderer extends TemplateRenderer {
	
	public ModuleInfoHibernateRenderer(IFeatureProject project) {
		super(project);
	}
	
	protected Map<String, Object> extractDataModel(WinVMJProduct product) {
		Map<String, Object> dataModel = new HashMap<>();
		
		dataModel.put("productPackage", product.getProductQualifiedName());
		dataModel.put("requiredModules", product.getModules());
		
		return dataModel;
	}
	
	protected String loadTemplateFilename() {
		return "module-info-hibernate";
	}
	
	protected IFile getOutputFile(WinVMJProduct product) {
		return project.getBuildFolder().getFolder(product.getProductQualifiedName()).getFile("module-info.java");
	}
}
