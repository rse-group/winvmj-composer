package de.ovgu.featureide.core.winvmj.core.impl;

import java.io.InputStreamReader;
import java.io.Reader;
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
	
	private static String getProductClassName(IFolder productModule) throws CoreException {
		IFolder dirToClass = productModule;
		for (String filePath: productModule.getName().split("\\."))
			dirToClass = dirToClass.getFolder(filePath);
		return FilenameUtils.getBaseName(dirToClass.members()[0].getName());
	}
	
	private static IFolder getProductModuleFromComposedProduct(IFeatureProject project)
			throws CoreException {
		IResource productModule = Stream
				.of(project.getBuildFolder().members())
				.filter(module -> module.getName().contains(".product."))
				.findFirst().get();
		
		return (IFolder) productModule;
	}
	
	private static List<IFolder> getModulesFromComposedProduct(IFeatureProject project) throws CoreException {
		return getModuleNamesFromComposedProduct(project).stream().map(mdl -> 
				project.getBuildFolder().getFolder(mdl)).collect(Collectors.toList());
	}
	
	private static List<String> getModuleNamesFromComposedProduct(IFeatureProject project)
			throws CoreException {
		List<String> sourceModules = Stream
				.of(project.getBuildFolder().members())
				.filter(module -> !module.getName().contains(".product."))
				.map(module -> module.getName())
				.collect(Collectors.toList());
		return selectModulesWithMapping(sourceModules, project.getProject());
	}
	
	private static Map<String,List<String>> getAllSplMappings(IProject 
			project) throws CoreException {
		Map<String, List<String>> mappings = new LinkedHashMap<String, List<String>>();
		for (IProject externalProject: project.getReferencedProjects()) {
			mappings.putAll(getAllSplMappings(externalProject));
		}
		Reader mapReader = new InputStreamReader(project
				.getFile(WinVMJComposer.FEATURE_MODULE_MAPPER_FILENAME).getContents());
		Gson gson = new Gson();
		mappings.putAll(gson.fromJson(mapReader,
				new TypeToken<LinkedHashMap<String, List<String>>>() {}.getType()));
		return mappings;
	}
	
	private static List<String> selectModulesWithMapping(List<String> sourceModules, 
			IProject project) throws CoreException {
		Map<String,List<String>> mappings = getAllSplMappings(project);
		return mappings.values().stream().distinct()
				.flatMap(modules -> modules.stream())
				.filter(module -> sourceModules.contains(module))
				.distinct().collect(Collectors.toList());
	}
}
