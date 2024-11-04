package de.ovgu.featureide.core.winvmj;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;

import org.apache.commons.io.FilenameUtils;

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
}
