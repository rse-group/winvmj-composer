package de.ovgu.featureide.core.winvmj.compile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;

import com.google.common.io.Files;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import de.ovgu.featureide.core.IFeatureProject;
import de.ovgu.featureide.core.winvmj.WinVMJComposer;
import de.ovgu.featureide.core.winvmj.internal.InternalResourceManager;
import de.ovgu.featureide.core.winvmj.runtime.WinVMJConsole;

public class SourceCompiler {
	public static void compileSource(IFeatureProject project) {
		try {
			importWinVMJLibraries(project);
			importDatabaseMappers(project);
			importWinVMJProductConfigs(project);
			importExternalLibraries(project);
			compileModules(project);
			//cleanBinaries(project);
		} catch (CoreException | IOException | URISyntaxException e) {
			e.printStackTrace();
		}
	}
	
	private static void cleanBinaries(IFeatureProject project) throws CoreException {
		IFolder binModuleFolder = project.getProject().getFolder("bin-comp");
		if (binModuleFolder.exists()) binModuleFolder.delete(true, null);
	}
	
	private static void importWinVMJProductConfigs(IFeatureProject project) 
			throws IOException, URISyntaxException, CoreException {
		IFolder srcGen = project.getProject().getFolder("src-gen");
		if (!srcGen.exists()) srcGen.create(false, true, null);
		WinVMJConsole.println("Unpack WinVMJ Configs for product...");
		InternalResourceManager.loadResourceDirectory("winvmj-configs", 
				srcGen.getLocation().toOSString());
		WinVMJConsole.println("WinVMJ Configs unpacked");
	}
	
	private static void importDatabaseMappers(IFeatureProject project) 
			throws IOException, URISyntaxException, CoreException {
		IFolder srcGen = project.getProject().getFolder("src-gen");
		if (!srcGen.exists()) srcGen.create(false, true, null);
		IFolder productModule = srcGen.getFolder(getProductModule(project));
		if (!productModule.exists()) productModule.create(false, true, null);
		WinVMJConsole.println("Importing database mappers for product...");
		InternalResourceManager.loadResourceDirectory("mappers", 
				productModule.getLocation().toOSString());
		WinVMJConsole.println("Database Mappers imported");
	}
	
	private static void importExternalLibraries(IFeatureProject project) 
			throws IOException, URISyntaxException, CoreException {
		IFolder externalModule = project.getProject().getFolder("external");
		IFolder srcGen = project.getProject().getFolder("src-gen");
		if (!srcGen.exists()) srcGen.create(false, true, null);
		IFolder productModule = srcGen.getFolder(getProductModule(project));
		if (!productModule.exists()) productModule.create(false, true, null);
		WinVMJConsole.println("Importing additional Libraries for product in external...");
		copy(externalModule, productModule);
		WinVMJConsole.println("Additional Libraries imported");
	}
	
	private static void importWinVMJLibraries(IFeatureProject project) 
			throws IOException, URISyntaxException, CoreException {
		IFolder srcGen = project.getProject().getFolder("src-gen");
		if (!srcGen.exists()) srcGen.create(false, true, null);
		IFolder productModule = srcGen.getFolder(getProductModule(project));
		if (!productModule.exists()) productModule.create(false, true, null);
		WinVMJConsole.println("Unpack WinVMJ Libraries for product...");
		InternalResourceManager.loadResourceDirectory("winvmj-libraries", 
				productModule.getLocation().toOSString());
		WinVMJConsole.println("WinVMJ Libraries unpacked");
	}
	
	private static String getProductModule(IFeatureProject project) throws CoreException {
		IResource productModule = Stream
				.of(project.getBuildFolder().members())
				.filter(module -> module.getName().contains(".product."))
				.findFirst().get();
		
		return productModule.getName();
	}
	
	private static List<String> getFeatureModules(IFeatureProject project) throws CoreException {
		Reader mapReader = null;
		try {
			mapReader = new InputStreamReader(project.getProject()
					.getFile(WinVMJComposer.FEATURE_MODULE_MAPPER_FILENAME)
					.getContents());
		} catch (CoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		Gson gson = new Gson();
		Map<String,List<String>> mappings = gson.fromJson(mapReader, 
				new TypeToken<LinkedHashMap<String, List<String>>>() {}.getType());
		
		List<String> sourceModules = Stream
				.of(project.getBuildFolder().members())
				.filter(module -> !module.getName().contains(".product."))
				.map(module -> module.getName())
				.collect(Collectors.toList());
		
		return mappings.values().stream()
				.flatMap(modules -> modules.stream())
				.filter(module -> sourceModules.contains(module))
				.collect(Collectors.toList());
	}
	
	private static void compileModules(IFeatureProject project) throws CoreException, IOException {
		String productModule = getProductModule(project);
		for (String module: getFeatureModules(project)) {
			compileModuleForProduct(project, module, productModule);
		}
		compileProductJar(project, productModule, getProductNameFromConfig(project));
	}
	
	private static String getProductNameFromConfig(IFeatureProject project) {
		return StringUtils.capitalize(Files.getNameWithoutExtension(project
				.getCurrentConfiguration().getFileName().toString()));
	}
	
	public static void compileProductJar(IFeatureProject project, 
			String productModuleName, String productName) throws IOException, CoreException {
		List<String> modulePaths = transverseModuleFilePaths(project
				.getBuildFolder().getFolder(productModuleName));
		IFolder compiledProductFolder = project.getProject().getFolder("src-gen").getFolder(productModuleName);
		IFolder binModuleFolder = project.getProject().getFolder("bin-comp").getFolder(productModuleName);
		
		List<String> compileCommand = new ArrayList<>();
		compileCommand.add("javac");
		compileCommand.add("-d");
		compileCommand.add(binModuleFolder.getLocation().toOSString());
		compileCommand.add("--module-path");
		compileCommand.add(compiledProductFolder.getLocation().toOSString());
		compileCommand.addAll(modulePaths);
		
		System.out.println(String.join(" ", compileCommand));

		ProcessBuilder compilePb = new ProcessBuilder(compileCommand);
		Process compileProcess = compilePb.start();
		BufferedReader reader = 
                new BufferedReader(new InputStreamReader(compileProcess.getInputStream()));
		String line = null;
		while ( (line = reader.readLine()) != null) System.out.println(line);
		reader.close();
		
		IFile compiledModuleFile = compiledProductFolder.getFile(productName + ".jar");
		System.out.println(compiledModuleFile.getLocation().toOSString());
		
		ProcessBuilder jarPb = new ProcessBuilder("jar", "--create", "--file", 
				compiledModuleFile.getLocation().toOSString(), 
				"--main-class", productModuleName + "." + productName,
				"-C", binModuleFolder.getLocation().toOSString(), ".");
		
		System.out.println("jar --create --file " + 
				compiledModuleFile.getLocation().toOSString() +
				" --main-class " + productModuleName + "." + productName +
				" -C " + binModuleFolder.getLocation().toOSString() + " .");
		WinVMJConsole.println("Compiling " + productModuleName + " product module...");
		Process jarProcess = jarPb.start();
		reader = new BufferedReader(new InputStreamReader(jarProcess.getInputStream()));
		line = null;
		while ( (line = reader.readLine()) != null) System.out.println(line);
		reader.close();
		WinVMJConsole.println(productModuleName + " product module packaged");
	}
	
	private static void compileModuleForProduct(IFeatureProject project, String module, 
			String productModule) throws CoreException, IOException {
		List<String> modulePaths = transverseModuleFilePaths(project.getBuildFolder().getFolder(module));
		IFolder compiledProductFolder = project.getProject().getFolder("src-gen").getFolder(productModule);
		IFolder binModuleFolder = project.getProject().getFolder("bin-comp").getFolder(module);
		
		List<String> compileCommand = new ArrayList<>();
		compileCommand.add("javac");
		compileCommand.add("-d");
		compileCommand.add(binModuleFolder.getLocation().toOSString());
		compileCommand.add("--module-path");
		compileCommand.add(compiledProductFolder.getLocation().toOSString());
		compileCommand.addAll(modulePaths);
		
		System.out.println(String.join(" ", compileCommand));

		ProcessBuilder compilePb = new ProcessBuilder(compileCommand);
		WinVMJConsole.println("Compiling " + module + " module...");
		Process compileProcess = compilePb.start();
		BufferedReader reader = 
                new BufferedReader(new InputStreamReader(compileProcess.getInputStream()));
		String line = null;
		while ( (line = reader.readLine()) != null) WinVMJConsole.println(line);
		reader.close();
		WinVMJConsole.println(module + " module compiled");
		
		IFile compiledModuleFile = compiledProductFolder.getFile(module + ".jar");
		System.out.println(compiledModuleFile.getLocation().toOSString());
		System.out.println("jar --create --file " + 
				compiledModuleFile.getLocation().toOSString() + 
				" -C " + binModuleFolder.getLocation().toOSString() + " .");
		ProcessBuilder jarPb = new ProcessBuilder("jar", "--create", "--file", 
				compiledModuleFile.getLocation().toOSString(), 
				"-C", binModuleFolder.getLocation().toOSString(), ".");
		WinVMJConsole.println("Packaging " + module + " module...");
		Process jarProcess = jarPb.start();
		reader = new BufferedReader(new InputStreamReader(jarProcess.getInputStream()));
		line = null;
		while ( (line = reader.readLine()) != null) WinVMJConsole.println(line);
		reader.close();
		WinVMJConsole.println(module + " module packaged");
	}
	
	private static List<String> transverseModuleFilePaths(IFolder module) throws CoreException {
		List<String> fileNames = new ArrayList<>();
		transverseModuleFilePaths(module, fileNames);
		return fileNames;
	}
	
	private static void transverseModuleFilePaths(IFolder submodule, List<String> fileNames) throws CoreException {
		for (IResource resource: submodule.members()) {
			if (resource instanceof IFolder) transverseModuleFilePaths((IFolder) resource, fileNames);
			else if (resource instanceof IFile && resource.getName().endsWith(".java")) 
				fileNames.add(quoteString(resource.getLocation().toOSString()));
		}
	}
	
	protected static void copy(IFolder featureFolder, IFolder buildFolder) throws CoreException {
		if (!featureFolder.exists()) {
			return;
		}

		for (final IResource res : featureFolder.members()) {
			if (res instanceof IFolder) {
				final IFolder folder = buildFolder.getFolder(res.getName());
				if (!folder.exists()) {
					folder.create(false, true, null);
				}
				copy((IFolder) res, folder);
			} else if (res instanceof IFile) {
				final IFile file = buildFolder.getFile(res.getName());
				if (!file.exists()) {
					res.copy(file.getFullPath(), true, null);
				}
			}
		}
	}
	
	private static String quoteString(String str) {
		return "\"" + str + "\"";
	}
}
