package de.ovgu.featureide.core.winvmj.compile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;

import de.ovgu.featureide.core.CorePlugin;
import de.ovgu.featureide.core.IFeatureProject;
import de.ovgu.featureide.core.winvmj.WinVMJComposer;
import de.ovgu.featureide.core.winvmj.core.WinVMJProduct;
import de.ovgu.featureide.core.winvmj.internal.InternalResourceManager;
import de.ovgu.featureide.core.winvmj.runtime.WinVMJConsole;
import de.ovgu.featureide.core.winvmj.templates.impl.HibernateCfgRenderer;
import de.ovgu.featureide.core.winvmj.templates.impl.RunScriptRenderer;
import de.ovgu.featureide.core.winvmj.templates.impl.SqliteDbPropertiesRenderer;

public class SourceCompiler {
	
	private static String OUTPUT_FOLDER = "src-gen";
	private SourceCompiler() {};
	
	public static void compileSource(IFeatureProject project) {
		try {
			WinVMJProduct sourceProduct = new WinVMJProduct(project);
			IFolder compiledProductDir = project.getProject().getFolder(OUTPUT_FOLDER);
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
		List<IResource> externalLibraries = listAllExternalLibraries(project);
		for (String module: product.getModules()) {
			importExternalLibrariesByModuleInfo(project, externalLibraries, compiledProductDir, product, module);
			compileModuleForProduct(project, compiledProductDir, module, productModule);
		}
		compileProductJar(project, compiledProductDir, productModule, product.getProductName());
	}
	
	private static List<IResource> listAllExternalLibraries(IFeatureProject project) throws CoreException {
		List<IResource> externalLibraries = new ArrayList<>();
		for (IProject externalProject: project.getProject().getReferencedProjects()) {
			CorePlugin.getDefault();
			externalLibraries.addAll(listAllExternalLibraries(CorePlugin.getFeatureProject(externalProject)));
		}
		externalLibraries.addAll(Arrays.asList(project.getProject()
				.getFolder(WinVMJComposer.EXTERNAL_LIB_FOLDERNAME)
				.members()));
		return externalLibraries;
	}
	
	private static void importExternalLibrariesByModuleInfo(IFeatureProject project, 
			List<IResource> externalLibs, IFolder compiledProductDir, WinVMJProduct product, 
			String module) throws IOException, CoreException {
		IFolder productModule = compiledProductDir.getFolder(product.getProductQualifiedName());
		List<String> requiredModules = parseModuleInfo(project, module);
		for (String requiredModule: requiredModules) {
			if (externalLibs.stream().anyMatch(el -> requiredModule.startsWith(requiredModule))) {
				IFile externalLib = (IFile) Stream.of(externalLibs)
						.filter(el -> requiredModule.startsWith(requiredModule))
						.findFirst().get();
				copyFile(externalLib, productModule);
			}
		}
	}
	
	private static void compileModuleForProduct(IFeatureProject project, IFolder productDir, 
			String module, String productModule) throws CoreException, IOException {
		IFolder compiledProductFolder = productDir.getFolder(productModule);
		IFolder binFolder = project.getProject().getFolder("bin-comp").getFolder(module);
		
		List<String> compileCommand = constructCompileCommand(project.getBuildFolder().getFolder(module), 
				binFolder, compiledProductFolder);
		
		System.out.println(String.join(" ", compileCommand));

		JavaCLI.execute("Compiling " + module + " module...", module + " module compiled", compileCommand);
		
		List<String> jarCommand = constructJARCommand(binFolder, compiledProductFolder, module);
		
		System.out.println(String.join(" ", jarCommand));
		
		JavaCLI.execute("Packaging " + module + " module...", module + " module packaged", jarCommand);
	}
	
	public static void compileProductJar(IFeatureProject project, IFolder productDir,
			String productModule, String productName) throws IOException, CoreException {
		IFolder compiledProductFolder = productDir.getFolder(productModule);
		IFolder binFolder = project.getProject().getFolder("bin-comp").getFolder(productModule);
		
		List<String> compileCommand = constructCompileCommand(project.getBuildFolder()
				.getFolder(productModule), binFolder, compiledProductFolder);
		
		System.out.println(String.join(" ", compileCommand));

		JavaCLI.execute("Compiling " + productModule + " module...", 
				productModule + " module compiled", compileCommand);
		
		String mainClass = productModule + "." + productName;
		
		List<String> jarCommand = constructJARCommand(binFolder, compiledProductFolder, productName, mainClass);
		
		System.out.println(String.join(" ", jarCommand));
		
		JavaCLI.execute("Compiling " + productModule + " product module...", 
				productModule + " product module packaged", jarCommand);
	}
	
	private static List<String> constructCompileCommand(IFolder sourceFolder, IFolder binFolder, 
			IFolder modulePath) throws CoreException {
		List<String> modulePaths = transverseModuleFilePaths(sourceFolder);
		
		List<String> compileCommand = new ArrayList<>();
		compileCommand.add("javac");
		compileCommand.add("-d");
		compileCommand.add(quoteString(binFolder.getLocation().toOSString()));
		compileCommand.add("--module-path");
		compileCommand.add(quoteString(modulePath.getLocation().toOSString()));
		compileCommand.addAll(modulePaths);
		
		return compileCommand;
	}
	
	private static List<String> constructJARCommand(IFolder binFolder, IFolder destFolder, 
			String jarName) throws CoreException {
		return constructJARCommand(binFolder, destFolder, jarName, "") ;
	}
	
	private static List<String> constructJARCommand(IFolder binFolder, IFolder destFolder, 
			String jarName, String mainClass) throws CoreException {
		IFile jarFile = destFolder.getFile(jarName + ".jar");
		
		List<String> jarCommand = new ArrayList<>();
		jarCommand.add("jar");
		jarCommand.add("--create");
		jarCommand.add("--file");
		jarCommand.add(quoteString(jarFile.getLocation().toOSString()));
		if (mainClass.length() > 0) {
			jarCommand.add("--main-class");
			jarCommand.add(mainClass);
		}
		jarCommand.add("-C");
		jarCommand.add(quoteString(binFolder.getLocation().toOSString()));
		jarCommand.add(".");
		
		return jarCommand;
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
	
	private static void copyFile(IFile file, IFolder outputFolder) throws CoreException {
		IFile copiedFile = outputFolder.getFile(file.getName());
		if (!copiedFile.exists()) copiedFile.create(file.getContents(), false, null);
		else copiedFile.setContents(file.getContents(), 1, null);
	}
	
	private static String quoteString(String str) {
		return "\"" + str + "\"";
	}
}
