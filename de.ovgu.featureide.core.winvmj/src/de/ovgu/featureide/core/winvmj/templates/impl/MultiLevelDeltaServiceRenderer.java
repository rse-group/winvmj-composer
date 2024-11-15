package de.ovgu.featureide.core.winvmj.templates.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
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

	private Map<String, Object> getRequiredMethods() {
		Map<String, Object> methods = new HashMap<>();

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

			WinVMJConsole.println("Isi file: " + content.toString());
		} catch (Exception e) {
			WinVMJConsole.println(e.getMessage());
			for (StackTraceElement em : e.getStackTrace())
				WinVMJConsole.println(em.toString());
			e.printStackTrace();
		}

		// try {
		// 	ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
		// 	Class<?> clazz = classLoader.loadClass(abstractComponentClassFqn);
		// 	WinVMJConsole.println("CLASS LOADER: " + clazz.toString());
			// Method[] declaredMethods = clazz.getDeclaredMethods();
			// List<String> abstractMethods = new ArrayList<>();

			// for (Method method : declaredMethods) {
			// 	if (Modifier.isAbstract(method.getModifiers())) {
			// 		StringBuilder methodSignature = new StringBuilder();
					
			// 		// Define method modified
			// 		if (Modifier.isPublic(method.getModifiers())) {
			// 			methodSignature.append("public ");
			// 		} else if (Modifier.isProtected(method.getModifiers())) {
			// 			methodSignature.append("protected ");
			// 		} else if (Modifier.isPrivate(method.getModifiers())) {
			// 			methodSignature.append("private ");
			// 		}
	
			// 		// Define method return type
			// 		methodSignature.append(method.getReturnType().getSimpleName()).append(" ");
	
			// 		// Define method name
			// 		methodSignature.append(method.getName()).append("(");
	
			// 		// Define method parameters
			// 		Class<?>[] parameterTypes = method.getParameterTypes();
			// 		for (int i = 0; i < parameterTypes.length; i++) {
			// 			methodSignature.append(parameterTypes[i].getSimpleName());
			// 			if (i < parameterTypes.length - 1) {
			// 				methodSignature.append(", ");
			// 			}
			// 		}

			// 		methodSignature.append(");");
			// 		abstractMethods.add(methodSignature.toString());
			// 	}
			// }

			// WinVMJConsole.println("ABSTRACT METHODS:");
			// for (String abstractMethod : abstractMethods) {
			// 	WinVMJConsole.println(abstractMethod);
			// }
		// } catch (Exception e) {
		// 	WinVMJConsole.println(e.getMessage());
		// 	for (StackTraceElement em : e.getStackTrace())
		// 		WinVMJConsole.println(em.toString());
		// 	e.printStackTrace();
		// }

		return methods;
	}

    private String[] getRequiredDeltas() {
        String[] deltas = new String[deltaModules.size()];
        for (int i = 0; i < deltaModules.size(); i++) {
            deltas[i] = deltaModules.get(i) + "." + featureName + "ServiceImpl";
        }
        return deltas;
    }
}
