package de.ovgu.featureide.core.winvmj.templates;

import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.runtime.CoreException;

import de.ovgu.featureide.core.IFeatureProject;
import de.ovgu.featureide.core.winvmj.core.WinVMJProduct;
import de.ovgu.featureide.core.winvmj.templates.TemplateRenderer;

public abstract class MultiLevelDeltaTemplateRenderer extends TemplateRenderer {
    protected String splName;
	protected String featureName;
    protected String featureFullyQualifiedName;
	protected String coreModule;
    private String layerType;

    public MultiLevelDeltaTemplateRenderer(
        IFeatureProject project,
        String splName,
		String featureName,
		String featureFullyQualifiedName,
		String coreModule,
        String layerType
    ) {
        super(project);
        this.splName = splName;
		this.featureName = featureName;
		this.featureFullyQualifiedName = featureFullyQualifiedName;
		this.coreModule = coreModule;
        this.layerType = layerType;
    }

    @Override
    protected abstract Map<String, Object> extractDataModel(WinVMJProduct product);

    @Override
	protected String loadTemplateFilename() {
        return layerType.equals("service")
                ? "MultiLevelDeltaServiceClass"
                : "MultiLevelDeltaResourceClass";
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
		
		for (String modulePath: featureFullyQualifiedName.split("\\.")) {
			featureModuleFolder = featureModuleFolder.getFolder(modulePath);
			if (!featureModuleFolder.exists())
				try {
					featureModuleFolder.create(false, true, null);
				} catch (CoreException e) {
					e.printStackTrace();
				}
		}
		
		return getLayerOutputFile(featureModuleFolder);
	}

    private IFile getLayerOutputFile(IFolder featureModuleFolder) {		
		featureModuleFolder = featureModuleFolder.getFolder(layerType);
		if (!featureModuleFolder.exists())
			try {
				featureModuleFolder.create(false, true, null);
			} catch (CoreException e) {
				e.printStackTrace();
			}
		
        String prefixClassName = layerType.equals("service")
                                    ? "ServiceImpl.java"
                                    : "ResourceImpl.java";
		return featureModuleFolder.getFile(featureName + prefixClassName);
    }
}
