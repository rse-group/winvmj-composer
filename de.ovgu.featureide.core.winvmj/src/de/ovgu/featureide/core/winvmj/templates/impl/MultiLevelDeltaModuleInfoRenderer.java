package de.ovgu.featureide.core.winvmj.templates.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.runtime.CoreException;

import de.ovgu.featureide.core.IFeatureProject;
import de.ovgu.featureide.core.winvmj.core.WinVMJProduct;

public class MultiLevelDeltaModuleInfoRenderer extends ModuleInfoRenderer {
    private String featureFullyQualifiedName;
    private List<String> requiredModules;

    public MultiLevelDeltaModuleInfoRenderer(
        IFeatureProject project,
        String featureFullyQualifiedName,
        List<String> requiredModules
    ) {
		super(project);
        this.featureFullyQualifiedName = featureFullyQualifiedName;
        this.requiredModules = requiredModules;
	}

    @Override
    protected Map<String, Object> extractDataModel(WinVMJProduct product) {
        Map<String, Object> dataModel = new HashMap<>();
		
		dataModel.put("productPackage", featureFullyQualifiedName);
		dataModel.put("requiredModules", requiredModules);
		dataModel.put("defaultAuthModel", checkDefaultAuthModel(product));
		
		return dataModel;
    }

    @Override
    protected IFile getOutputFile(WinVMJProduct product) {
        IFolder featureModuleFolder = project.getBuildFolder()
				.getFolder(featureFullyQualifiedName);
		
		if (!featureModuleFolder.exists())
			try {
				featureModuleFolder.create(false, true, null);
			} catch (CoreException e) {
				e.printStackTrace();
			}
        
		return featureModuleFolder.getFile("module-info.java");
	}
}
