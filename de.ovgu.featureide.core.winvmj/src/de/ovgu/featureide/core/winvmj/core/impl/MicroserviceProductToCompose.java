package de.ovgu.featureide.core.winvmj.core.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.runtime.CoreException;
import org.logicng.io.parsers.ParserException;

import de.ovgu.featureide.core.IFeatureProject;
import de.ovgu.featureide.core.winvmj.Utils;
import de.ovgu.featureide.fm.core.base.IFeature;

public class MicroserviceProductToCompose extends ProductToCompose {
	
	public MicroserviceProductToCompose(IFeatureProject featureProject, String productName, List<IFeature> features,  
			Map<String, IFolder> allModulesMapping, IFolder messagingModule) {
		super();
	    this.productName = productName;
		this.splName = Utils.getSplName(featureProject);
		try {
			List<IFolder> featureModules = selectModules(featureProject, features);
			this.modules = resolveDependencies(featureModules, allModulesMapping);
			this.modules.add(messagingModule);
		} catch (CoreException | ParserException | IOException e) {
			this.modules = null;
		}
	}
	
	public List<IFolder> resolveDependencies(List<IFolder> selectedFeatures,
	        Map<String, IFolder> allModuleMapping) throws CoreException, IOException {

	    List<IFolder> resolvedFeatures = new ArrayList<>();
	    Set<IFolder> visited = new HashSet<>();
	    Deque<IFolder> stack = new LinkedList<>(selectedFeatures);

	    while (!stack.isEmpty()) {
	        IFolder current = stack.peek();
	        if (visited.contains(current)) {
	            stack.pop();
	            continue;
	        }

	        boolean dependenciesResolved = true;
	        IFile moduleInfoFile = current.getFile("module-info.java");

	        if (moduleInfoFile.exists()) {
	            List<String> dependencies = extractModuleInfoDependencies(moduleInfoFile);
	            for (String dependency : dependencies) {
	                IFolder depFolder = allModuleMapping.get(dependency);
	                if (depFolder != null && !visited.contains(depFolder)) {
	                    dependenciesResolved = false;
	                    stack.push(depFolder);
	                }
	            }
	        }

	        if (dependenciesResolved) {
	            visited.add(current);
	            resolvedFeatures.add(current);
	            stack.pop();
	        }
	    }

	    return resolvedFeatures;
	}
	
    private List<String> extractModuleInfoDependencies(IFile moduleInfoFile) throws IOException, CoreException {
        List<String> dependencies = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(moduleInfoFile.getContents()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.startsWith("requires vmj")) {
                	// Stop processing once we encounter an WinVMJ Libraries dependency
                    break;
                } else if (line.startsWith("requires ")) {
                    String dependency = line.split(" ")[1].replace(";", "").trim();
                    dependencies.add(dependency);
                }
            }
        }

        return dependencies;
    }
}
