package de.ovgu.featureide.core.winvmj.compile;

import java.io.BufferedReader;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import de.ovgu.featureide.core.CorePlugin;
import de.ovgu.featureide.core.IFeatureProject;
import de.ovgu.featureide.core.winvmj.Utils;
import de.ovgu.featureide.core.winvmj.WinVMJComposer;
import de.ovgu.featureide.core.winvmj.core.WinVMJProduct;
import de.ovgu.featureide.core.winvmj.core.impl.ComposedProduct;
import de.ovgu.featureide.core.winvmj.internal.InternalResourceManager;
import de.ovgu.featureide.core.winvmj.runtime.WinVMJConsole;
import de.ovgu.featureide.core.winvmj.templates.impl.CorsPropertiesRenderer;
import de.ovgu.featureide.core.winvmj.templates.impl.DeploymentScriptRenderer;
import de.ovgu.featureide.core.winvmj.templates.impl.EndpointsConfigRenderer;
import de.ovgu.featureide.core.winvmj.templates.impl.HibernatePropertiesRenderer;
import de.ovgu.featureide.core.winvmj.templates.impl.RunScriptRenderer;
import de.ovgu.featureide.core.winvmj.templates.impl.UnixDeploymentScriptRenderer;
import de.ovgu.featureide.core.winvmj.templates.impl.UnixRunAllScriptRenderer;
import de.ovgu.featureide.core.winvmj.templates.impl.UnixRunScriptRenderer;
import de.ovgu.featureide.core.winvmj.templates.impl.WindowsDeploymentScriptRenderer;
import de.ovgu.featureide.core.winvmj.templates.impl.WindowsRunScriptRenderer;

public class SourceCompiler {

	private static String OUTPUT_FOLDER = "src-gen";
	private static String OUTPUT_MODULES_FOLDER = "modules-gen";
	private static String MODULES_FOLDER = "modules";

	private SourceCompiler() {
	};

	public static void compileSource(IFeatureProject project) {
		try {
			WinVMJProduct sourceProduct = new ComposedProduct(project);
			IFolder compiledProductDir = project.getProject().getFolder(OUTPUT_FOLDER);
			if (!compiledProductDir.exists())
				compiledProductDir.create(false, true, null);
			compiledProductDir = compiledProductDir.getFolder(sourceProduct.getProductName());
			if (!compiledProductDir.exists())
				compiledProductDir.create(false, true, null);
			importWinVMJLibraries(compiledProductDir, sourceProduct);
			importWinVMJProductConfigs(compiledProductDir);
			generateConfigFiles(project, sourceProduct);
			compileModules(project, compiledProductDir, sourceProduct);
			insertSqlFolder(compiledProductDir, project);
		} catch (CoreException | IOException | URISyntaxException e) {
			e.printStackTrace();
		}
	}

	public static void compileModulesSource(IFeatureProject project) throws URISyntaxException {
		try {
			IFolder compiledModulesDir = project.getProject().getFolder(OUTPUT_MODULES_FOLDER);
			if (!compiledModulesDir.exists())
				compiledModulesDir.create(false, true, null);
			IFolder modulesDir = project.getProject().getFolder(MODULES_FOLDER);

			IFolder librariesDir = project.getProject().getFolder("winvmj-libraries");
			IFolder externalDir = project.getProject().getFolder("external");

			importWinVMJLibrariesForModules(compiledModulesDir);
			compileInternalModules(project, compiledModulesDir, modulesDir);
			deleteLibraries(compiledModulesDir, librariesDir);
			deleteExternal(compiledModulesDir, externalDir);
		} catch (CoreException | IOException e) {
			e.printStackTrace();
		}
	}

	public static void compileModuleSource(IFolder moduleFromSrcFolder, IFeatureProject project)
	        throws URISyntaxException {
        IFolder librariesDir = project.getProject().getFolder("winvmj-libraries");
        IFolder externalDir = project.getProject().getFolder("external");
	    try {
	        IFolder compiledModulesDir = project.getProject().getFolder(OUTPUT_MODULES_FOLDER);
	        if (!compiledModulesDir.exists())
	            compiledModulesDir.create(false, true, null);

	        IFile compiledJar = compiledModulesDir.getFile(moduleFromSrcFolder.getName() + ".jar");

	        long moduleLastModified = getLastModifiedTime(moduleFromSrcFolder);
	        long jarLastModified = compiledJar.exists() ? compiledJar.getLocalTimeStamp() : -1;

	        
	        Set<String> requirements = extractDelta(project, moduleFromSrcFolder);
	        boolean requiresUpdate = false;

	        for (String require : requirements) {
	            IFolder requireFolder = project.getProject().getFolder(MODULES_FOLDER).getFolder(require);
	            if (requireFolder.exists()) {
	                IFile generatedJar = compiledModulesDir.getFile(require + ".jar");

	                long requireLastModified = getLastModifiedTime(requireFolder);
	                long requireJarLastModified = generatedJar.exists() ? generatedJar.getLocalTimeStamp() : -1;

	                if (!generatedJar.exists() || requireLastModified > requireJarLastModified) {
	                    WinVMJConsole.println("Required module " + require + " is outdated. Recompiling...");
	                    compileModuleSource(requireFolder, project);
	                    requiresUpdate = true; 
	                }
	            }
	        }
	        if (requiresUpdate || moduleLastModified > jarLastModified) {
	            WinVMJConsole.println("Compiling module " + moduleFromSrcFolder.getName() + "...");
	            List<IResource> externalLibraries = listAllExternalLibraries(project);
	            importWinVMJLibrariesForModules(compiledModulesDir);
	            importExternalLibrariesByModuleInfoForModules(project, externalLibraries, compiledModulesDir, moduleFromSrcFolder);
	            compileModuleForProduct(project, compiledModulesDir, moduleFromSrcFolder, "compileModule");
	            cleanBinaries(project);
	        } else {
	            WinVMJConsole.println("Module " + moduleFromSrcFolder.getName() + " is up-to-date. Skipping compilation.");
	        }
	        
            deleteLibraries(compiledModulesDir, librariesDir);
            deleteExternal(compiledModulesDir, externalDir);
	    } catch (CoreException | IOException e) {
	        e.printStackTrace();
	    }
	}

	private static void importWinVMJLibrariesForModules(IFolder compiledModulesDir)
			throws IOException, URISyntaxException, CoreException {
		WinVMJConsole.println("Unpack WinVMJ Libraries for product...");
		InternalResourceManager.loadResourceDirectory("winvmj-libraries",
				compiledModulesDir.getLocation().toOSString());
		WinVMJConsole.println("WinVMJ Libraries unpacked");
	}

	private static List<String> parseModuleInfo(IFeatureProject project, IFolder module)
			throws IOException, CoreException {
		IFile moduleInfo = module.getFile("module-info.java");
		List<String> requiredModules = new ArrayList<>();
		BufferedReader reader = new BufferedReader(new InputStreamReader(moduleInfo.getContents()));
		String line = null;
		while ((line = reader.readLine()) != null) {
			String trimmedLine = line.trim();
			if (trimmedLine.startsWith("requires")) {
				String[] moduleStatement = trimmedLine.split(" ");
				requiredModules.add(moduleStatement[moduleStatement.length - 1].replace(";", ""));
			}
		}
		reader.close();
		return requiredModules;
	}

	private static void generateConfigFiles(IFeatureProject project, WinVMJProduct product)
			throws CoreException, IOException {
		Properties dbProperties = new Properties();
		dbProperties.load(project.getProject().getFile(WinVMJComposer.DB_CONFIG_FILENAME).getContents());

		String dbUsername = dbProperties.getProperty("db.username");
		String dbPassword = dbProperties.getProperty("db.password");
		
		WinVMJConsole.println("Generating additional config files for product...");

		new HibernatePropertiesRenderer(project, dbUsername, dbPassword).render(product);
		new CorsPropertiesRenderer(project).render(product);
		// new RunScriptRenderer(project, dbUsername, dbPassword).render(product);
		new WindowsRunScriptRenderer(project, dbUsername, dbPassword).render(product);
		new UnixRunScriptRenderer(project, dbUsername, dbPassword).render(product);
		// new DeploymentScriptRenderer(project).render(product);
		new WindowsDeploymentScriptRenderer(project).render(product);
		new UnixDeploymentScriptRenderer(project).render(product);
		new UnixRunAllScriptRenderer(project, dbUsername, dbPassword).render(product);
		new EndpointsConfigRenderer(project).render(product);

		WinVMJConsole.println("All additional config files has been generated");
	}

	private static void cleanBinaries(IFeatureProject project) throws CoreException {
		IFolder binModuleFolder = project.getProject().getFolder("bin-comp");
		if (binModuleFolder.exists())
			binModuleFolder.delete(true, null);
	}

	private static void importWinVMJProductConfigs(IFolder compiledProductDir) throws IOException, CoreException {
		WinVMJConsole.println("Unpack WinVMJ Configs for product...");
		InternalResourceManager.loadResourceDirectory("winvmj-configs", compiledProductDir.getLocation().toOSString());
		WinVMJConsole.println("WinVMJ Configs unpacked");
	}

	private static void importWinVMJLibraries(IFolder compiledProductDir, WinVMJProduct product)
			throws IOException, URISyntaxException, CoreException {
		IFolder productModule = compiledProductDir.getFolder(product.getProductQualifiedName());
		if (!productModule.exists())
			productModule.create(false, true, null);
		WinVMJConsole.println("Unpack WinVMJ Libraries for product...");
		InternalResourceManager.loadResourceDirectory("winvmj-libraries", productModule.getLocation().toOSString());
		WinVMJConsole.println("WinVMJ Libraries unpacked");
	}

	private static void compileModules(IFeatureProject project, IFolder compiledProductDir, WinVMJProduct product)
			throws CoreException, IOException {
		String productModule = product.getProductQualifiedName();

		IFolder generatedModulesDir = project.getProject().getFolder(OUTPUT_MODULES_FOLDER);
		List<IResource> externalLibraries = listAllExternalLibraries(project);
		for (IFolder module : product.getModules()) {

			boolean isJarCopied = false;

			isJarCopied = copyInternalJarsByModuleName(project, generatedModulesDir, compiledProductDir, product,
					module);

			if (isJarCopied) {
				WinVMJConsole.println("Module " + module.getName() + " copied to product folder");
			} else {
				importExternalLibrariesByModuleInfo(project, externalLibraries, compiledProductDir, product, module);
				compileModuleForProduct(project, compiledProductDir, module, productModule);
			}

		}
		compileProductJar(project, compiledProductDir, productModule, product.getProductName());
		cleanBinaries(project);
	}

	public static void deleteLibraries(IFolder compiledModulesDir, IFolder winvmjLibrariesDir) throws CoreException {
		compiledModulesDir.refreshLocal(IFolder.DEPTH_INFINITE, null);

		Set<String> baseNames = new HashSet<>();
		for (IResource resource : winvmjLibrariesDir.members()) {
			if (resource.getType() == IResource.FILE && resource.getName().endsWith(".jar")) {
				String baseName = getBaseNameFromFile(resource.getName());
				baseNames.add(baseName);
			}
		}

		Set<String> filesToDelete = new HashSet<>(
				Arrays.asList("cas.client.jar", "commons-logging-1.2.jar", "sqlite.jdbc.jar"));

		for (IResource resource : compiledModulesDir.members()) {
			if (resource.getType() == IResource.FILE && resource.getName().endsWith(".jar")) {
				String baseName = getBaseNameFromFile(resource.getName());
				if (baseNames.contains(baseName) || filesToDelete.contains(resource.getName())) {
					resource.delete(true, null);
				}
			}
		}
	}

	private static void deleteExternal(IFolder compiledModulesDir, IFolder externalDir)
			throws CoreException, IOException {
		compiledModulesDir.refreshLocal(IFolder.DEPTH_INFINITE, null);

		Set<String> baseNames = new HashSet<>();
		for (IResource resource : externalDir.members()) {
			if (resource.getType() == IResource.FILE && resource.getName().endsWith(".jar")) {
				String baseName = getBaseNameFromFile(resource.getName());
				baseNames.add(baseName);
			}
		}
		for (IResource resource : compiledModulesDir.members()) {
			if (resource.getType() == IResource.FILE && resource.getName().endsWith(".jar")) {
				String baseName = getBaseNameFromFile(resource.getName());
				if (baseNames.contains(baseName)) {
					resource.delete(true, null);
				}
			}
		}
	}

	private static boolean copyInternalJarsByModuleName(IFeatureProject project, IFolder generatedModulesDir,
			IFolder compiledProductDir, WinVMJProduct product, IFolder module) throws CoreException, IOException {

		if (!generatedModulesDir.exists()) {
			return false;
		}

		IResource[] internalResources = generatedModulesDir.members();
		IFolder productModule = compiledProductDir.getFolder(product.getProductQualifiedName());

		String moduleName = module.getName();

		for (IResource internalResource : internalResources) {
			if (internalResource instanceof IFile && internalResource.getName().endsWith(".jar")) {
				String internalBaseName = getBaseNameFromFile(internalResource.getName());

				if (internalBaseName.equals(moduleName)) {
					System.out.println(
							"Copying internal JAR: " + internalResource.getName() + " to " + productModule.getName());
					copyFile((IFile) internalResource, productModule);
					return true;
				}
			}
		}
		return false;
	}

	private static String getBaseNameFromFile(String fileName) {
		int dashIndex = fileName.lastIndexOf('-');
		int dotIndex = fileName.lastIndexOf('.');
		if (dashIndex > 0 && dotIndex > dashIndex) {
			return fileName.substring(0, dashIndex);
		}
		return fileName.substring(0, dotIndex);
	}

	private static List<IFolder> getModulesFromComposedProduct(IFeatureProject featureProject) throws CoreException {
		List<String> moduleOrders = getModuleOrdersByMappings(featureProject, featureProject.getProject());
		List<IFolder> orderedSourceModules = new ArrayList<>();
		for (String module : moduleOrders) {
			orderedSourceModules.add(featureProject.getProject().getFolder("modules").getFolder(module));
		}

		return orderedSourceModules;
	}

	private static List<String> getModuleOrdersByMappings(IFeatureProject featureProject, IProject project)
			throws CoreException {
		List<String> moduleOrders = new ArrayList<>();

		Reader mapReader = new InputStreamReader(
				project.getFile(WinVMJComposer.FEATURE_MODULE_MAPPER_FILENAME).getContents());
		Gson gson = new Gson();
		Map<String, List<String>> splMappings = gson.fromJson(mapReader,
				new TypeToken<LinkedHashMap<String, List<String>>>() {
				}.getType());

		for (Entry<String, List<String>> mapping : splMappings.entrySet()) {
			String key = mapping.getKey();
			List<String> value = mapping.getValue();

			if (Utils.isMultiLevelDelta(mapping)) {
				String multiLevelDeltaModule = changeDeltaModule(value.get(0), key.toLowerCase(), featureProject);
				value.add(multiLevelDeltaModule);
			}

			moduleOrders.addAll(value);
		}

		return moduleOrders.stream().distinct().collect(Collectors.toList());
	}

	private static String changeDeltaModule(String module, String deltaName, IFeatureProject featureProject) {
		String[] splittedModule = module.split("\\.");
		String splName = splittedModule[0];
		String featureName = splittedModule[1];
		String multiLevelDeltaModule = String.format("%s.%s.%s", splName, featureName, deltaName);

		IFolder moduleFolder = featureProject.getBuildFolder().getFolder(multiLevelDeltaModule + featureName);
		if (moduleFolder.exists())
			multiLevelDeltaModule += featureName;

		return multiLevelDeltaModule;
	}

	private static void compileInternalModules(IFeatureProject project, IFolder compiledModulesDir, IFolder modulesDir)
			throws CoreException, IOException {
		IResource[] moduleResources = modulesDir.members();
		List<IResource> externalLibraries = listAllExternalLibraries(project);

		List<IFolder> modulesFromFeatureJSON = getModulesFromComposedProduct(project);

		Set<IResource> filteredModule = new LinkedHashSet<>();
		filteredModule.addAll(modulesFromFeatureJSON.stream()
				.filter(folder -> Arrays.stream(moduleResources)
						.anyMatch(resource -> resource.getName().equals(folder.getName())))
				.collect(Collectors.toList()));
		
		filteredModule.addAll(Arrays.asList(moduleResources));
		
		for (IResource resource : filteredModule) {
			if (resource instanceof IFolder) {
				IFolder moduleFolder = (IFolder) resource;
				IFile compiledJar = compiledModulesDir.getFile(moduleFolder.getName() + ".jar");

				long moduleLastModified = getLastModifiedTime(moduleFolder);
				long jarLastModified = compiledJar.exists() ? compiledJar.getLocalTimeStamp() : -1;

				if (compiledJar.exists() && moduleLastModified <= jarLastModified) {
					WinVMJConsole.println(
							"Module " + moduleFolder.getName() + " has already been generated and is up-to-date");
					System.out.println(
							"Module " + moduleFolder.getName() + " has already been generated and is up-to-date");
					continue;
				}

				if (!(moduleFolder.getName().contains("product.template"))) {
					importExternalLibrariesByModuleInfoForModules(project, externalLibraries, compiledModulesDir,
							moduleFolder);
					compileModuleForProduct(project, compiledModulesDir, moduleFolder, "");
				}

			}
		}

		cleanBinaries(project);
	}

	private static long getLastModifiedTime(IFolder folder) throws CoreException {
		long latestTimestamp = folder.getLocalTimeStamp();
		for (IResource resource : folder.members()) {
			if (resource instanceof IFile) {
				latestTimestamp = Math.max(latestTimestamp, resource.getLocalTimeStamp());
			} else if (resource instanceof IFolder) {
				latestTimestamp = Math.max(latestTimestamp, getLastModifiedTime((IFolder) resource));
			}
		}
		return latestTimestamp;
	}

	private static void importExternalLibrariesByModuleInfoForModules(IFeatureProject project,
			List<IResource> externalLibs, IFolder compiledModulesDir, IFolder module)
			throws IOException, CoreException {
		List<String> requiredModules = parseModuleInfo(project, module);
		for (String requiredModule : requiredModules) {
			Stream.of(externalLibs).forEach(els -> {
				els.forEach(el -> {
					try {
						copyFile((IFile) el, compiledModulesDir);
					} catch (CoreException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				});
			});
		}
	}

	private static List<IResource> listAllExternalLibraries(IFeatureProject project) throws CoreException {
		List<IResource> externalLibraries = new ArrayList<>();
		for (IProject externalProject : project.getProject().getReferencedProjects()) {
			CorePlugin.getDefault();
			externalLibraries.addAll(listAllExternalLibraries(CorePlugin.getFeatureProject(externalProject)));
		}
		externalLibraries.addAll(
				Arrays.asList(project.getProject().getFolder(WinVMJComposer.EXTERNAL_LIB_FOLDERNAME).members()));
		return externalLibraries;
	}

	private static void importExternalLibrariesByModuleInfo(IFeatureProject project, List<IResource> externalLibs,
			IFolder compiledProductDir, WinVMJProduct product, IFolder module) throws IOException, CoreException {
		IFolder productModule = compiledProductDir.getFolder(product.getProductQualifiedName());
		List<String> requiredModules = parseModuleInfo(project, module);
		for (String requiredModule : requiredModules) {
			Stream.of(externalLibs).forEach(els -> {
				els.forEach(el -> {
					try {
						copyFile((IFile) el, productModule);
					} catch (CoreException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				});
			});
		}
	}

	private static void compileModuleForProduct(IFeatureProject project, IFolder productDir, IFolder module,
			String productModule) throws CoreException, IOException {
		IFolder compiledFolder;

		if (productModule != "" && productModule != "compileModule") {
			compiledFolder = productDir.getFolder(productModule);
		} else {
			compiledFolder = productDir;
		}

		IFolder binFolder = project.getProject().getFolder("bin-comp").getFolder(module.getName());

		List<String> compileCommand = constructCompileCommand(project, module, binFolder, compiledFolder, productModule);

		System.out.println(String.join(" ", compileCommand));

		try {
		    JavaCLI.execute("Compiling " + module + " module...", module + " module compiled", compileCommand);
		} catch (Exception e) {
			WinVMJConsole.println("Compilation failed for module: " + module + ". Skipping packaging.");
		    return;
		}

		List<String> jarCommand = constructJARCommand(binFolder, compiledFolder, module.getName());

		System.out.println(String.join(" ", jarCommand));

		JavaCLI.execute("Packaging " + module + " module...", module + " module packaged", jarCommand);
	}

	public static void compileProductJar(IFeatureProject project, IFolder productDir, String productModule,
			String productName) throws IOException, CoreException {
		IFolder compiledProductFolder = productDir.getFolder(productModule);
		IFolder binFolder = project.getProject().getFolder("bin-comp").getFolder(productModule);

		List<String> compileCommand = constructCompileCommand(project, project.getBuildFolder().getFolder(productModule),
				binFolder, compiledProductFolder, "");

		System.out.println(String.join(" ", compileCommand));

		JavaCLI.execute("Compiling " + productModule + " module...", productModule + " module compiled",
				compileCommand);

		String mainClass = productModule + "." + productName;

		List<String> jarCommand = constructJARCommand(binFolder, compiledProductFolder, productName, mainClass);

		System.out.println(String.join(" ", jarCommand));

		JavaCLI.execute("Compiling " + productModule + " product module...", productModule + " product module packaged",
				jarCommand);
	}

	private static List<String> constructCompileCommand(IFeatureProject project, IFolder sourceFolder, IFolder binFolder, IFolder modulePath, String handleRequirements)
			throws CoreException, IOException {

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
	
	private static Set<String> extractDelta(IFeatureProject project, IFolder moduleFolder) throws CoreException {
	    Set<String> dependencies = new HashSet<>();
	    
	    IFile moduleInfoFile = moduleFolder.getFile("module-info.java");
	    if (moduleInfoFile.exists()) {
	        try (BufferedReader reader = new BufferedReader(new InputStreamReader(moduleInfoFile.getContents()))) {
	            String line;
	            while ((line = reader.readLine()) != null) {
	                line = line.trim();
	                if (line.startsWith("requires ")) {
	                    String[] parts = line.split("\\s+");
	                    
	                    String requiredModule = parts.length == 3 ? parts[2] : parts[1];
	                    requiredModule = requiredModule.replace(";", "");
	                    
	                    if (project.getProject().getFolder("modules").getFolder(requiredModule).exists()) {
	                        dependencies.add(requiredModule);
	                    }
	                }
	            }
	        } catch (IOException e) {
	            e.printStackTrace();
	        }
	    }

	    return dependencies;
	}

	private static List<String> constructJARCommand(IFolder binFolder, IFolder destFolder, String jarName)
			throws CoreException {
		return constructJARCommand(binFolder, destFolder, jarName, "");
	}

	private static List<String> constructJARCommand(IFolder binFolder, IFolder destFolder, String jarName,
			String mainClass) throws CoreException {
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
		for (IResource resource : submodule.members()) {
			if (resource instanceof IFolder)
				transverseModuleFilePaths((IFolder) resource, fileNames);
			else if (resource instanceof IFile && resource.getName().endsWith(".java"))
				fileNames.add(quoteString(resource.getLocation().toOSString()));
		}
	}

	private static void copyFile(IFile file, IFolder outputFolder) throws CoreException {
		IFile copiedFile = outputFolder.getFile(file.getName());
		if (!copiedFile.exists())
			copiedFile.create(file.getContents(), false, null);
		else
			copiedFile.setContents(file.getContents(), 1, null);
	}

	private static boolean isWindows() {
		return System.getProperty("os.name").toLowerCase().contains("win");
	}

	private static String quoteString(String str) {
		if (isWindows())
			return "\"" + str + "\"";
		return "/" + str;
	}

	private static void insertSqlFolder(IFolder compiledProductDir, IFeatureProject project)
			throws IOException, CoreException {
		IFolder sqlFolder = project.getProject().getFolder("sql");
		WinVMJConsole.println("Insert SQL Files");
		IFolder sqlDestinationFolder = compiledProductDir.getFolder("sql");
		if (!sqlDestinationFolder.exists())
			sqlDestinationFolder.create(false, true, null);
		for (IResource resource : sqlFolder.members()) {
			copyFile((IFile) resource, compiledProductDir.getFolder("sql"));
		}
		WinVMJConsole.println("SQL Files inserted");
	}
}
