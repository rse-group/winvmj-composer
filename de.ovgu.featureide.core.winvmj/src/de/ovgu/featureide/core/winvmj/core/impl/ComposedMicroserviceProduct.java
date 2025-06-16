package de.ovgu.featureide.core.winvmj.core.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.runtime.CoreException;
import de.ovgu.featureide.core.IFeatureProject;

public class ComposedMicroserviceProduct extends ComposedProduct {
	public ComposedMicroserviceProduct(IFeatureProject project, IFolder productModule)
			throws CoreException {
		super(project,productModule);
		this.modules = getModulesFromComposedProduct(project, productModule);
	}
	
	private List<IFolder> getModulesFromComposedProduct(IFeatureProject featureProject, IFolder productModule) throws CoreException {
		IFile moduleInfoFile = productModule.getFile("module-info.java");
		List<String> dependenciesModule = new ArrayList<>();
		try {
	        dependenciesModule = extractModuleInfoDependencies(moduleInfoFile);
	    } catch (IOException e) {
	        e.printStackTrace();
	    }
		List<IFolder> featureModules = new ArrayList<>();
		
		for (String moduleName : dependenciesModule) {
			IFolder module = featureProject.getBuildFolder().getFolder(moduleName);
		    if (!module.exists()) {
		        continue;
		    }
		    
		    if (moduleName.equals("vmj.messaging")) {
		    	featureModules.add(0,module);
		    } else {
		    	featureModules.add(module);
		    }
		}
		
	    return featureModules;
	}
	
	
	private List<String> extractModuleInfoDependencies(IFile moduleInfoFile) throws IOException, CoreException {
        List<String> dependencies = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(moduleInfoFile.getContents()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.startsWith("requires ")) {
                    String dependency = line.split(" ")[1].replace(";", "").trim();
                    dependencies.add(dependency);
                }
            }
        }

        return dependencies;
    }
}
