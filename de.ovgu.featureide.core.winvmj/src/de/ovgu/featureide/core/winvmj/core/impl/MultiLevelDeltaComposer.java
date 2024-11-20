package de.ovgu.featureide.core.winvmj.core.impl;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;

import de.ovgu.featureide.core.IFeatureProject;
import de.ovgu.featureide.core.winvmj.Utils;
import de.ovgu.featureide.core.winvmj.core.WinVMJProduct;
import de.ovgu.featureide.core.winvmj.runtime.WinVMJConsole;
import de.ovgu.featureide.core.winvmj.templates.impl.MultiLevelDeltaModuleInfoRenderer;
import de.ovgu.featureide.core.winvmj.templates.impl.MultiLevelDeltaResourceRenderer;
import de.ovgu.featureide.core.winvmj.templates.impl.MultiLevelDeltaServiceRenderer;

public class MultiLevelDeltaComposer {
	private IFeatureProject project;
	private WinVMJProduct product;
	private String featureName;
    private String featureVariationName;
	private String splName;
	private String coreModule;
    private List<String> deltaModules;
    
    public MultiLevelDeltaComposer(
		IFeatureProject project,
		WinVMJProduct product, 
		String featureVariationName,
		List<String> deltaModules
	) {
		this.project = project;
		this.product = product;
		this.featureVariationName = featureVariationName;
        this.deltaModules = deltaModules;

		String[] splittedDeltaPackage = deltaModules.get(0).split("\\.");
		this.splName = splittedDeltaPackage[0];

		try {
			String subDirectory = "resource";
			this.coreModule = splName + "." + splittedDeltaPackage[1] + ".core";
			List<String> coreClasses = Utils.getAllClassInModule(
				project, coreModule, subDirectory);
			for (int i = 0; i < coreClasses.size(); i++) {
				String className = coreClasses.get(i);
				String capitalizedSubDirectory = subDirectory.substring(
					0, 1).toUpperCase() + subDirectory.substring(1);
				if (className.endsWith(capitalizedSubDirectory + "Component")) {
					this.featureName = className.split(capitalizedSubDirectory + "Component")[0];
					break;
				}
			}
		} catch (CoreException e) {
			WinVMJConsole.println(e.getMessage());
			for (StackTraceElement em : e.getStackTrace())
				WinVMJConsole.println(em.toString());
			e.printStackTrace();
		}
    }
	
	public void compose() {
		// Compose module-info.java
		List<String> requiredModules = new ArrayList<>(deltaModules);
		requiredModules.add(0, coreModule);
		MultiLevelDeltaModuleInfoRenderer moduleInfoRenderer = new MultiLevelDeltaModuleInfoRenderer(
			project, 
			getFeatureFullyQualifiedName(), 
			requiredModules
		);
		moduleInfoRenderer.render(product);

		// Compose service layer
		MultiLevelDeltaServiceRenderer serviceRenderer = new MultiLevelDeltaServiceRenderer(
			project,
			splName,
			featureName,
			getFeatureFullyQualifiedName(),
			coreModule,
			deltaModules
		);
		serviceRenderer.render(product);

		// Compose resource layer
		MultiLevelDeltaResourceRenderer resourceRenderer = new MultiLevelDeltaResourceRenderer(
			project,
			splName,
			featureName,
			getFeatureFullyQualifiedName(),
			coreModule
		);
		resourceRenderer.render(product);
	}

    private String getFeatureName(String featurePackage) {
    	String[] splittedFeaturePackage = featurePackage.split("\\.");
    	return splittedFeaturePackage[splittedFeaturePackage.length - 1];
    }

	public String getFeatureFullyQualifiedName() {
		return splName.toLowerCase() + "." + featureName.toLowerCase() + "." + featureVariationName.toLowerCase();
	}
}
