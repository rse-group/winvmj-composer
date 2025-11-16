package de.ovgu.featureide.core.winvmj.templates.impl;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IFile;

import de.ovgu.featureide.core.IFeatureProject;
import de.ovgu.featureide.core.winvmj.core.WinVMJProduct;
import de.ovgu.featureide.core.winvmj.templates.TemplateRenderer;
public class SettingsGradleRenderer extends TemplateRenderer{
    public SettingsGradleRenderer(IFeatureProject project) {
        super(project);
    }
    protected IFile getOutputFile(WinVMJProduct product) {
		return project.getProject().getFolder("src-gen")
				.getFolder(product.getProductName())
				.getFile(loadTemplateFilename());
	}

    protected Map<String, Object> extractDataModel(WinVMJProduct product) {
        Map<String, Object> dataModel = new HashMap<>();

		dataModel.put("product", product.getProductQualifiedName());
        dataModel.put("productName", product.getProductName());
		return dataModel;
    }
    
    protected String loadTemplateFilename() {
        return "settings.gradle";
    }
}
