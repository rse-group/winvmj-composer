package de.ovgu.featureide.core.winvmj.templates.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.List;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.ClassLoader;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import org.checkerframework.checker.units.qual.cd;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.runtime.CoreException;

import de.ovgu.featureide.core.IFeatureProject;
import de.ovgu.featureide.core.winvmj.Utils;
import de.ovgu.featureide.core.winvmj.core.WinVMJProduct;
import de.ovgu.featureide.core.winvmj.runtime.WinVMJConsole;
import de.ovgu.featureide.core.winvmj.templates.TemplateRenderer;

public class MultiLevelDeltaServiceRenderer extends TemplateRenderer {
	private String splName;
	private String featureName;
    private String featureFullyQualifiedName;
	private String coreModule;
	private List<String> deltaModules;
	private String baseComponent;
	private int initialDeltaIndex = 0;
    
    public MultiLevelDeltaServiceRenderer(
		IFeatureProject project,
		String splName,
		String featureName,
		String featureFullyQualifiedName,
		String coreModule,
		List<String> deltaModules
	) {
		super(project);
		this.splName = splName;
		this.featureName = featureName;
		this.featureFullyQualifiedName = featureFullyQualifiedName;
		this.coreModule = coreModule;
		this.deltaModules = deltaModules;
	}

    @Override
	protected Map<String, Object> extractDataModel(WinVMJProduct product) {
		Map<String, Object> dataModel = new HashMap<>();
		
		dataModel.put("splName", splName);
		dataModel.put("featureName", featureName);
		dataModel.put("package", featureFullyQualifiedName);
		dataModel.put("deltas", getRequiredDeltas());
		dataModel.put("methods", getRequiredMethods());

		getRequiredBindings();
		dataModel.put("baseComponent", baseComponent);
		dataModel.put("initialDeltaIndex", initialDeltaIndex);

		return dataModel;
	}

    @Override
	protected String loadTemplateFilename() {
		return "MultiLevelDeltaServiceClass";
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
		
		featureModuleFolder = featureModuleFolder.getFolder("service");
		if (!featureModuleFolder.exists())
			try {
				featureModuleFolder.create(false, true, null);
			} catch (CoreException e) {
				e.printStackTrace();
			}
		
		return featureModuleFolder.getFile(featureName + "ServiceImpl.java");
	}

	private void getRequiredBindings() {
		try {
			String corePackage = splName + "." + featureName.toLowerCase() + ".core";
			List<String> coreClasses = Utils.getAllClassInModule(
				project, corePackage, "service");
			boolean coreModuleHasConcreteComponent = false;

			for (int i = 0; i < coreClasses.size(); i++) {
				String className = coreClasses.get(i);
				if (className.endsWith("ServiceImpl")) 
					coreModuleHasConcreteComponent = true;
			}
			
			if (coreModuleHasConcreteComponent) {
				baseComponent = corePackage;
			} else {
				baseComponent = deltaModules.get(0);
				initialDeltaIndex = 1;
			}
			baseComponent += "." + featureName + "ServiceImpl";
		} catch (CoreException e) {
			WinVMJConsole.println(e.getMessage());
			for (StackTraceElement em : e.getStackTrace())
				WinVMJConsole.println(em.toString());
			e.printStackTrace();
		}
	}

	private List<Map<String, Object>> getRequiredMethods() {
		List<Map<String, Object>> methods = new ArrayList<>();
		Map<String, Object> method;

		String abstractComponentClassFqn = "src/" + coreModule + "/";
		for (String coreModulePath: coreModule.split("\\.")) {
			abstractComponentClassFqn += coreModulePath + "/";
		}
		abstractComponentClassFqn += "service/" + featureName + "ServiceComponent.java";
		
		IFile abstractComponentClass = project.getProject().getFile(abstractComponentClassFqn);
		try (Reader mapReader = new InputStreamReader(abstractComponentClass.getContents())) {
			char[] buffer = new char[1024];
			int numRead;
			StringBuilder content = new StringBuilder();

			while ((numRead = mapReader.read(buffer)) != -1) {
				content.append(buffer, 0, numRead);
			}

			Pattern pattern = Pattern.compile(
				"(\\b(public|protected|private)?\\b\\s*)?abstract\\s+([\\w<>,\\s]+)\\s+(\\w+)\\((.*?)\\);"
			);
			Matcher matcher = pattern.matcher(content.toString());

			while (matcher.find()) {
				method = new HashMap<>();
				String methodSignature = "";
				String visibility = matcher.group(2) != null ? matcher.group(2) : "";
				String returnType = matcher.group(3).trim();
				String methodName = matcher.group(4);
				String parameters = matcher.group(5).trim();

				if (!visibility.equals("")) methodSignature += visibility + " ";
				methodSignature += String.format(
					"%s %s(%s)", 
					returnType,
					methodName,
					parameters
				);

				method.put("signature", methodSignature);
				method.put("returnType", returnType);
				method.put("methodName", methodName);
				method.put("parameterNames", getParameterNames(parameters));

				methods.add(method);
            }
			WinVMJConsole.println();
		} catch (Exception e) {
			WinVMJConsole.println(e.getMessage());
			for (StackTraceElement em : e.getStackTrace())
				WinVMJConsole.println(em.toString());
			e.printStackTrace();
		}
		return methods;
	}

    private String[] getRequiredDeltas() {
        String[] deltas = new String[deltaModules.size()];
        for (int i = 0; i < deltaModules.size(); i++) {
            deltas[i] = deltaModules.get(i) + "." + featureName + "ServiceImpl";
        }
        return deltas;
    }

	private String getParameterNames(String parameters) {
		String parameterNames = "";
		String[] splittedParameters = parameters.split(",(?![^<>]*>)");

		for (int i = 0; i < splittedParameters.length; i++) {
			String param = splittedParameters[i].trim();
			String[] parts = param.split("\\s+");
			String paramName = parts[parts.length - 1].replaceAll(
				".*\\s+([\\w]+)$", "$1");
			
			if (i > 0) parameterNames += ", ";
			parameterNames += paramName;
		}

		return parameterNames;
	}
}
