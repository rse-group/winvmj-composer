package de.ovgu.featureide.core.winvmj;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.CoreException;
import org.logicng.datastructures.Assignment;
import org.logicng.formulas.Formula;
import org.logicng.formulas.FormulaFactory;
import org.logicng.formulas.Variable;
import org.logicng.io.parsers.ParserException;
import org.logicng.io.parsers.PropositionalParser;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;

import org.apache.commons.io.FilenameUtils;

import de.ovgu.featureide.fm.core.base.IFeature;
import de.ovgu.featureide.core.IFeatureProject;

public class Utils {
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

	private static String getFeatureName(String module) {
		String[] moduleParts = module.split("\\.");
		return moduleParts[1];
	}
}
