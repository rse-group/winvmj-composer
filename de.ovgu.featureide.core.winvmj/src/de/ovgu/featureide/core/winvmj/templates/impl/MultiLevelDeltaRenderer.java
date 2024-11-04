package de.ovgu.featureide.core.winvmj.templates.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.CoreException;

import de.ovgu.featureide.core.IFeatureProject;
import de.ovgu.featureide.core.winvmj.Utils;
import de.ovgu.featureide.core.winvmj.core.WinVMJProduct;
import de.ovgu.featureide.core.winvmj.runtime.WinVMJConsole;
import de.ovgu.featureide.core.winvmj.templates.TemplateRenderer;
import de.ovgu.featureide.core.winvmj.templates.impl.ProductClassRenderer;

public class MultiLevelDeltaRenderer extends TemplateRenderer {
    private String featureFullQualifiedName;
    private String featureLineName;
	private String splName;
	private String businessLayer = "Service";
	private String concreteClassName;
	private String baseComponent;
	private int initialDeltaIndex = 0;
    private List<String> deltaModules;
    
    public MultiLevelDeltaRenderer(IFeatureProject project) {
		super(project);
	}

    @Override
	protected Map<String, Object> extractDataModel(WinVMJProduct product) {
		Map<String, Object> dataModel = new HashMap<>();
		
		dataModel.put("splName", splName);
		getRequiredProductBindings(dataModel);
        dataModel.put("deltas", getRequiredDeltas());
		dataModel.put("businessLayer", businessLayer);

		return dataModel;
	}

    @Override
	protected String loadTemplateFilename() {
		return "MultiLevelDeltaClass";
	}

    @Override
	protected IFile getOutputFile(WinVMJProduct product) {  	      
		IFolder productModuleFolder = project.getBuildFolder()
				.getFolder(featureFullQualifiedName);
		
		try {
			productModuleFolder.create(false, true, null);
		} catch (CoreException e) {
			e.printStackTrace();
		}
		
		for (String modulePath: featureFullQualifiedName.split("\\.")) {
			productModuleFolder = productModuleFolder.getFolder(modulePath);
			if (!productModuleFolder.exists())
				try {
					productModuleFolder.create(false, true, null);
				} catch (CoreException e) {
					e.printStackTrace();
				}
		}
		
		return productModuleFolder.getFile(concreteClassName + ".java");
	}

    public void setFeatureConfiguration(String featureName, List<String> deltaModules) {
        String[] splittedDeltaModule = deltaModules.get(0).split("\\.");
    	String modifiedFeatureName = featureName;
    	for (int i = 0; i < deltaModules.size(); i++) {
			if (modifiedFeatureName.toLowerCase().equals(getProductName(deltaModules.get(i)))) {
				modifiedFeatureName = "Generated" + modifiedFeatureName;
    		}
    	}
		
		this.splName = splittedDeltaModule[0] + "." + splittedDeltaModule[1];
        this.featureFullQualifiedName = splName + "." + modifiedFeatureName.toLowerCase();
        this.deltaModules = deltaModules;
    }

	private void getRequiredProductBindings(Map<String, Object> dataModel) {
		try {
			dataModel.put("featurePackage", featureFullQualifiedName);

			String coreModule = splName + ".core";
			List<String> coreClasses = Utils.getAllClassInModule(
				project, coreModule, businessLayer.toLowerCase());

			boolean coreModuleHasConcreteComponent = false;
			for (int i = 0; i < coreClasses.size(); i++) {
				String className = coreClasses.get(i);
				if (className.endsWith(businessLayer + "Impl")) 
					coreModuleHasConcreteComponent = true;
				if (className.endsWith(businessLayer + "Component")) {
					this.featureLineName = className.split(businessLayer + "Component")[0];
					this.concreteClassName = featureLineName + businessLayer + "Impl";
					dataModel.put("feature", featureLineName);
				}
			}
			
			if (coreModuleHasConcreteComponent) {
				baseComponent = coreModule;
			} else {
				baseComponent = deltaModules.get(0);
				initialDeltaIndex = 1;
			}
			baseComponent += "." + concreteClassName;
			dataModel.put("baseComponent", baseComponent);
			dataModel.put("initialDeltaIndex", initialDeltaIndex);
		} catch (CoreException e) {
			WinVMJConsole.println(e.getMessage());
			for (StackTraceElement em : e.getStackTrace())
				WinVMJConsole.println(em.toString());
			e.printStackTrace();
		}
	}

    private String[] getRequiredDeltas() {
        String[] deltas = new String[deltaModules.size()];
        for (int i = 0; i < deltaModules.size(); i++) {
            deltas[i] = deltaModules.get(i) + "." + concreteClassName;
        }
        return deltas;
    }
    
    private String getProductName(String productModule) {
    	String[] splittedProductModule = productModule.split("\\.");
    	return splittedProductModule[splittedProductModule.length - 1];
    }
}
