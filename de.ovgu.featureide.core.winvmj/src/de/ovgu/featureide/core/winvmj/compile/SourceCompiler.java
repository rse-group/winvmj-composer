package de.ovgu.featureide.core.winvmj.compile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;

import de.ovgu.featureide.core.IFeatureProject;
import de.ovgu.featureide.core.winvmj.WinVMJComposer;
import de.ovgu.featureide.core.winvmj.core.WinVMJProduct;
import de.ovgu.featureide.core.winvmj.internal.InternalResourceManager;
import de.ovgu.featureide.core.winvmj.runtime.WinVMJConsole;
import de.ovgu.featureide.core.winvmj.templates.impl.HibernateCfgRenderer;
import de.ovgu.featureide.core.winvmj.templates.impl.RunScriptRenderer;
import de.ovgu.featureide.core.winvmj.templates.impl.SqliteDbPropertiesRenderer;

public class SourceCompiler {
	private SourceCompiler() {};
	
	public static void compileSource(IFeatureProject project) {
		IFile featureModuleMapper = project.getProject().getFile(WinVMJComposer.FEATURE_MODULE_MAPPER_FILENAME);
		try {
			WinVMJProduct sourceProduct = new WinVMJProduct(project, featureModuleMapper);
			IFolder compiledProductDir = project.getProject().getFolder("src-gen");
			if (!compiledProductDir.exists()) compiledProductDir.create(false, true, null);
			compiledProductDir = compiledProductDir.getFolder(sourceProduct.getProductName());
			if (!compiledProductDir.exists()) compiledProductDir.create(false, true, null);
			importWinVMJLibraries(compiledProductDir, sourceProduct);
			importDatabaseMappers(project, compiledProductDir, sourceProduct);
			importWinVMJProductConfigs(compiledProductDir);
			generateConfigFiles(project, sourceProduct);
			compileModules(project, compiledProductDir, sourceProduct);
			cleanBinaries(project);
		} catch (CoreException | IOException | URISyntaxException e) {
			e.printStackTrace();
		}
	}
	
	private static List<String> parseModuleInfo(IFeatureProject project, String module) 
			throws IOException, CoreException {
		IFile moduleInfo = project.getProject().getFolder("modules")
				.getFolder(module).getFile("module-info.java");
		List<String> requiredModules = new ArrayList<>();
		BufferedReader reader = 
                new BufferedReader(new InputStreamReader(moduleInfo.getContents()));
		String line = null;
		while ( (line = reader.readLine()) != null) {
			String trimmedLine = line.trim();
			if (trimmedLine.startsWith("requires")) {
				String[] moduleStatement = trimmedLine.split(" ");
				requiredModules.add(moduleStatement[moduleStatement.length-1].replace(";", ""));
			}
		}
		reader.close();
		return requiredModules;
	}
	
	private static void generateConfigFiles(IFeatureProject project,
			WinVMJProduct product) throws CoreException {
		WinVMJConsole.println("Generating additional config files for product...");
		new HibernateCfgRenderer(project).render(product);
		new SqliteDbPropertiesRenderer(project).render(product);
		new RunScriptRenderer(project).render(product);
		WinVMJConsole.println("All additional config files has been generated");
	}
	
	
	private static void cleanBinaries(IFeatureProject project) throws CoreException {
		IFolder binModuleFolder = project.getProject().getFolder("bin-comp");
		if (binModuleFolder.exists()) binModuleFolder.delete(true, null);
	}
	
	private static void importWinVMJProductConfigs(IFolder compiledProductDir) 
			throws IOException, CoreException {
		WinVMJConsole.println("Unpack WinVMJ Configs for product...");
		InternalResourceManager.loadResourceDirectory("winvmj-configs", 
				compiledProductDir.getLocation().toOSString());
		WinVMJConsole.println("WinVMJ Configs unpacked");
	}
	
	private static void importDatabaseMappers(IFeatureProject project, 
			IFolder compiledProductDir, WinVMJProduct product) 
			throws IOException, CoreException {
		IFolder mappers = project.getProject().getFolder("mappers");
		IFolder productModule = compiledProductDir
				.getFolder(product.getProductQualifiedName());
		List<IResource> requiredMappers = Stream.of(mappers.members())
				.filter(f -> product.getModules()
				.stream().anyMatch(m -> m.contains(f.getName().split("_")[0])))
		.collect(Collectors.toList());
		requiredMappers.forEach(m -> WinVMJConsole.println(m.getName()));
		if (!productModule.exists()) productModule.create(false, true, null);
		WinVMJConsole.println("Importing database mappers for product...");
		for (IResource mapperFile: requiredMappers)
			copyFile((IFile)mapperFile, productModule);
		WinVMJConsole.println("Database Mappers imported");
	}
	
	private static void importWinVMJLibraries(IFolder compiledProductDir, WinVMJProduct product) 
			throws IOException, URISyntaxException, CoreException {
		IFolder productModule = compiledProductDir.getFolder(product.getProductQualifiedName());
		if (!productModule.exists()) productModule.create(false, true, null);
		WinVMJConsole.println("Unpack WinVMJ Libraries for product...");
		InternalResourceManager.loadResourceDirectory("winvmj-libraries", 
				productModule.getLocation().toOSString());
		WinVMJConsole.println("WinVMJ Libraries unpacked");
	}
	
	private static void compileModules(IFeatureProject project, IFolder compiledProductDir, 
			WinVMJProduct product) throws CoreException, IOException {
		String productModule = product.getProductQualifiedName();
		for (String module: product.getModules()) {
			importExternalLibrariesByModuleInfo(project, compiledProductDir, product, module);
			compileModuleForProduct(project, compiledProductDir, module, productModule);
		}
		compileProductJar(project, compiledProductDir, productModule, product.getProductName());
	}
	
	private static void importExternalLibrariesByModuleInfo(IFeatureProject project, 
			IFolder compiledProductDir, WinVMJProduct product, String module) 
			throws IOException, CoreException {
		IResource[] externalLibs = project.getProject()
				.getFolder(WinVMJComposer.EXTERNAL_LIB_FOLDERNAME).members();
		IFolder productModule = compiledProductDir.getFolder(product.getProductQualifiedName());
		List<String> requiredModules = parseModuleInfo(project, module);
		for (String requiredModule: requiredModules) {
			if (Stream.of(externalLibs).anyMatch(el -> requiredModule.startsWith(requiredModule))) {
				IFile externalLib = (IFile) Stream.of(externalLibs)
						.filter(el -> requiredModule.startsWith(requiredModule))
						.findFirst().get();
				copyFile(externalLib, productModule);
			}
		}
	}
	
	public static void compileProductJar(IFeatureProject project, IFolder compiledProductDir,
			String productModuleName, String productName) throws IOException, CoreException {
		List<String> modulePaths = transverseModuleFilePaths(project
				.getBuildFolder().getFolder(productModuleName));
		IFolder compiledProductFolder = compiledProductDir.getFolder(productModuleName);
		IFolder binModuleFolder = project.getProject().getFolder("bin-comp").getFolder(productModuleName);
		
		List<String> compileCommand = new ArrayList<>();
		compileCommand.add("javac");
		compileCommand.add("-d");
		compileCommand.add(quoteString(binModuleFolder.getLocation().toOSString()));
		compileCommand.add("--module-path");
		compileCommand.add(quoteString(compiledProductFolder.getLocation().toOSString()));
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
				quoteString(compiledModuleFile.getLocation().toOSString()), 
				"--main-class", productModuleName + "." + productName,
				"-C", quoteString(binModuleFolder.getLocation().toOSString()), ".");
		
		System.out.println("jar --create --file " + 
				quoteString(compiledModuleFile.getLocation().toOSString()) +
				" --main-class " + productModuleName + "." + productName +
				" -C " + quoteString(binModuleFolder.getLocation().toOSString()) + " .");
		WinVMJConsole.println("Compiling " + productModuleName + " product module...");
		Process jarProcess = jarPb.start();
		reader = new BufferedReader(new InputStreamReader(jarProcess.getInputStream()));
		line = null;
		while ( (line = reader.readLine()) != null) System.out.println(line);
		reader.close();
		WinVMJConsole.println(productModuleName + " product module packaged");
	}
	
	private static void compileModuleForProduct(IFeatureProject project, IFolder compiledProductDir, 
			String module, String productModule) throws CoreException, IOException {
		List<String> modulePaths = transverseModuleFilePaths(project.getBuildFolder().getFolder(module));
		IFolder compiledProductFolder = compiledProductDir.getFolder(productModule);
		IFolder binModuleFolder = project.getProject().getFolder("bin-comp").getFolder(module);
		
		List<String> compileCommand = new ArrayList<>();
		compileCommand.add("javac");
		compileCommand.add("-d");
		compileCommand.add(quoteString(binModuleFolder.getLocation().toOSString()));
		compileCommand.add("--module-path");
		compileCommand.add(quoteString(compiledProductFolder.getLocation().toOSString()));
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
	
	private static void copy(IFolder featureFolder, IFolder buildFolder) throws CoreException {
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
	
	private static void copyFile(IFile file, IFolder outputFolder) throws CoreException {
		IFile copiedFile = outputFolder.getFile(file.getName());
		if (!copiedFile.exists()) copiedFile.create(file.getContents(), false, null);
		else copiedFile.setContents(file.getContents(), 1, null);
	}
	
	private static String quoteString(String str) {
		return "\"" + str + "\"";
	}
}
