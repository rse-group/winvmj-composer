package de.ovgu.featureide.core.winvmj.templates.impl;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;

import de.ovgu.featureide.core.IFeatureProject;
import de.ovgu.featureide.core.winvmj.core.WinVMJProduct;
import de.ovgu.featureide.core.winvmj.templates.TemplateRenderer;

public class CorsPropertiesRenderer extends TemplateRenderer {
	
	
	public CorsPropertiesRenderer(IFeatureProject project) {
		super(project);
	}
	
	@Override
	public void render(WinVMJProduct product) {
		IFile outputFile = getOutputFile(product);
		try {
			outputFile.refreshLocal(IFile.DEPTH_ZERO, null);
		} catch (CoreException e) {
			e.printStackTrace();
		}
		if (!outputFile.exists()) {
			super.render(product);
		}
	}
	
	protected Map<String, Object> extractDataModel(WinVMJProduct product) {
		Map<String, Object> dataModel = new HashMap<>();
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
