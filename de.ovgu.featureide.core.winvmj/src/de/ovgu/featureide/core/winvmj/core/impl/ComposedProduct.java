package de.ovgu.featureide.core.winvmj.core.impl;

import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.io.FilenameUtils;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import de.ovgu.featureide.core.IFeatureProject;
import de.ovgu.featureide.core.winvmj.WinVMJComposer;
import de.ovgu.featureide.core.winvmj.core.WinVMJProduct;

public class ComposedProduct extends WinVMJProduct {
	
	public ComposedProduct(IFeatureProject project)
			throws CoreException {
		IFolder productModule = getProductModuleFromComposedProduct(project);
		this.splName = productModule.getName().split(".product.")[0];
		this.productName = getProductClassName(productModule);
		this.modules = getModulesFromComposedProduct(project);
		
	}
	
	private String getProductClassName(IFolder productModule) throws CoreException {
		IFolder dirToClass = productModule;
		for (String filePath: productModule.getName().split("\\."))
			dirToClass = dirToClass.getFolder(filePath);
		
		IResource[] folderMembers = dirToClass.members();
		String productFileName = "";
		for (int i = 0; i < folderMembers.length; i++) {
			if (folderMembers[i].getName().endsWith(".java")) {
				productFileName = folderMembers[i].getName();
				break;
			}
		}

		return FilenameUtils.getBaseName(productFileName);
	}
	
	private IFolder getProductModuleFromComposedProduct(IFeatureProject project)
			throws CoreException {
		IResource productModule = Stream
				.of(project.getBuildFolder().members())
				.filter(module -> module.getName().contains(".product."))
				.findFirst().get();
		
		return (IFolder) productModule;
	}
	
	private List<IFolder> getModulesFromComposedProduct(IFeatureProject project) throws CoreException {
		List<String> moduleOrders = getModuleOrdersByMappings(project.getProject());
		List<IFolder> orderedSourceModules = new ArrayList<>();
		List<IFolder> sourceModules = Stream.of(project.getBuildFolder().members())
				.filter(m -> m instanceof IFolder)
				.map(m -> (IFolder) m).collect(Collectors.toList());
		for (String module: moduleOrders)
			if (containsModule(sourceModules, module)) 
				orderedSourceModules.add(project.getBuildFolder().getFolder(module));
			
		return orderedSourceModules;
	}
	
	private boolean containsModule(List<IFolder> modules, String module) {
		return modules.stream().anyMatch(m -> m.getName().equals(module));
	}
	
	private List<String> getModuleOrdersByMappings(IProject 
			project) throws CoreException {
		List<String> moduleOrders = new ArrayList<>();
		for (IProject externalProject: project.getReferencedProjects()) {
			moduleOrders.addAll(getModuleOrdersByMappings(externalProject));
		}
		
		Reader mapReader = new InputStreamReader(project
				.getFile(WinVMJComposer.FEATURE_MODULE_MAPPER_FILENAME).getContents());
		Gson gson = new Gson();
		Map<String, List<String>> splMappings = gson.fromJson(mapReader,
				new TypeToken<LinkedHashMap<String, List<String>>>() {}.getType());
		splMappings.values().forEach(v -> moduleOrders.addAll(v));
		return moduleOrders.stream().distinct().collect(Collectors.toList());
	}
}
