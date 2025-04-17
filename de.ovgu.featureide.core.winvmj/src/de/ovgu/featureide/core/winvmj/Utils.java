package de.ovgu.featureide.core.winvmj;

import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.core.runtime.CoreException;
import org.logicng.datastructures.Assignment;
import org.logicng.formulas.Formula;
import org.logicng.formulas.FormulaFactory;
import org.logicng.formulas.Variable;
import org.logicng.io.parsers.ParserException;
import org.logicng.io.parsers.PropositionalParser;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;

import org.apache.commons.io.FilenameUtils;

import de.ovgu.featureide.fm.core.base.IFeature;
import de.ovgu.featureide.core.IFeatureProject;
import de.ovgu.featureide.core.winvmj.runtime.WinVMJConsole;
import de.ovgu.featureide.fm.core.io.EclipseFileSystem;

public class Utils {
	public static List<String> getRelatedProducts(IFeatureProject project, 
			String externalSplName, String productName) throws CoreException {
		IFile interSplProductMapper = project.getProject()
				.getFile(WinVMJComposer.INTER_SPL_PRODUCT_MAPPER_FILENAME);
		Reader mapReader =  new InputStreamReader(interSplProductMapper.getContents());
		Gson gson = new Gson();
		Map<String, List<String>> mappings;
		try {
			mappings = gson.fromJson(mapReader, 
					new TypeToken<LinkedHashMap<String, List<String>>>() {}.getType());
		} catch (NullPointerException e) {
			mappings = new LinkedHashMap<String, List<String>>();
		}
		if (mappings.containsKey(productName))
			return mappings.get(productName).stream()
				.filter(p -> p.startsWith(externalSplName)).map(p -> 
				p.replace(externalSplName + ":", ""))
				.collect(Collectors.toList());
		return new ArrayList<>();
	}

	public static List<IFeature> selectFeaturesFromRelatedProducts(String externalSplName, 
			IFeatureProject refProject, List<String> relatedProducts) 
					throws CoreException {
		List<IFeature> features = new ArrayList<>();
		for (String relatedProduct: relatedProducts) {
			Optional<IResource> configFile = Stream.of(refProject.getConfigFolder()
					.members()).filter(c -> c.getName().startsWith(relatedProduct))
					.findFirst();
			if (configFile.isPresent())
				features.addAll(refProject.loadConfiguration(EclipseFileSystem
						.getPath(configFile.get())).getSelectedFeatures());
			else WinVMJConsole.println("[WARNING] Product `" + relatedProduct + 
					"` does not exist in `" + externalSplName + "` SPL and will be ignored");
		}
		
		return features.stream().distinct().collect(Collectors.toList());
	}

    public static List<String> getAllClassInModule(
        IFeatureProject project, String module, 
        String... subDirectories) throws CoreException {
		IFolder moduleFolder = project.getBuildFolder().getFolder(module);
		for (String modulePath : module.split("\\.")) {
			moduleFolder = moduleFolder.getFolder(modulePath);
		}
		for (String subDir : subDirectories) {
			moduleFolder = moduleFolder.getFolder(subDir);
		}
		List<String> classNames = new ArrayList<>();
		for (IResource classFile : moduleFolder.members()) {
			if (classFile.getName().endsWith(".java")) {
				classNames.add(FilenameUtils.getBaseName(classFile.getName()));
			}
		}
		return classNames;
	}

	public static boolean isMultiLevelDelta(Entry<String, List<String>> featureToModule) {
		List<String> modules = featureToModule.getValue();
		List<String> finalizedModules = modules.stream()
				.filter(module -> !module.contains(".core"))
				.collect(Collectors.toList());
		
		if (finalizedModules.size() <= 1) return false;
		
		String featureName = "";
		for (String module: finalizedModules) {
			if (featureName.equals("")) {
				featureName = getFeatureName(module);
			} else if (!featureName.equals(getFeatureName(module))) {
				return false;
			}
		}

		return true;
	}

	public static boolean evaluate(Assignment assignment, PropositionalParser formulaParser, 
			String formulaString) throws ParserException {
		Formula formula = formulaParser.parse(formulaString);
		return formula.evaluate(assignment);
	}

	public static Assignment getFeatureCheckingAssignment(List<IFeature> features, 
			FormulaFactory formulaFactory) {
		List<Variable> featureVariables = features.stream().map(feature -> 
		formulaFactory.variable(feature.getName())).collect(Collectors.toList());
		return new Assignment(featureVariables);
	}

	public static String getSplName(IFeatureProject project) {
		return project.getFeatureModel().getStructure().getRoot().getFeature().getName();
	}
	
	public static Map<String, IFolder> getAllModulesMapping(IFeatureProject project) throws CoreException {
		List<IProject> allProjects = new ArrayList<>();
	    allProjects.add(project.getProject()); 
	    Collections.addAll(allProjects, project.getProject().getReferencedProjects()); 

	    // collect all module folders from all projects
	    Map<String, IFolder> allModuleFolders = new HashMap<>();
	    for (IProject p : allProjects) {
	        IFolder moduleFolder = p.getFolder(WinVMJComposer.MODULE_FOLDERNAME);
	        if (moduleFolder.exists()) {
	            for (IResource resource : moduleFolder.members()) {
	                if (resource instanceof IFolder) {
	                    allModuleFolders.put(resource.getName(), (IFolder) resource);
	                }
	            }
	        }
	    }
	    return allModuleFolders;
	}

	private static String getFeatureName(String module) {
		String[] moduleParts = module.split("\\.");
		return moduleParts[1];
	}
}
