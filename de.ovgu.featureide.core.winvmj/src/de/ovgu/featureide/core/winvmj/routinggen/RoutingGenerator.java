package de.ovgu.featureide.core.winvmj.routinggen;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Stream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import de.ovgu.featureide.core.CorePlugin;
import de.ovgu.featureide.core.IFeatureProject;
import de.ovgu.featureide.core.winvmj.WinVMJComposer;
import de.ovgu.featureide.core.winvmj.compile.JavaCLI;
import de.ovgu.featureide.core.winvmj.core.WinVMJProduct;
import de.ovgu.featureide.core.winvmj.core.impl.ComposedProduct;
import de.ovgu.featureide.core.winvmj.internal.InternalResourceManager;
import de.ovgu.featureide.core.winvmj.runtime.WinVMJConsole;
import de.ovgu.featureide.core.winvmj.templates.impl.HibernatePropertiesRenderer;
import de.ovgu.featureide.core.winvmj.templates.impl.MenuComponentRenderer;
import de.ovgu.featureide.core.winvmj.templates.impl.RunScriptRenderer;

public class RoutingGenerator {
	
	private static String OUTPUT_FOLDER = "routing";
	private static DocumentBuilder documentBuilder;
	private RoutingGenerator() {};
	
	public static void generateRouting(IFeatureProject project) throws IOException {
		try {
			WinVMJProduct sourceProduct = new ComposedProduct(project);
			IFolder compiledProductDir = project.getProject().getFolder(OUTPUT_FOLDER);
			if (!compiledProductDir.exists()) compiledProductDir.create(false, true, null);
			compiledProductDir = compiledProductDir.getFolder(sourceProduct.getProductName());
			WinVMJConsole.println("Before config file");
//			File configFile = new File(project.getCurrentConfiguration().toString());
//			Document document = documentBuilder.parse(configFile);
//			document.getDocumentElement().normalize();
//			Element configElement = document.getDocumentElement();
			
			Element featureMapElement = readXmlFile(project.getProject().getFile("FeatureMapping.xml").getLocation().toString());
			Map<String, Object> featureMap = mapFeature(featureMapElement.getElementsByTagName("feature"));
			WinVMJConsole.println("Done reading mapping");
			
			Element configElement = readXmlFile(project.getCurrentConfiguration().toFile());
			WinVMJConsole.println(configElement.getNodeName());
			NodeList featureList = configElement.getElementsByTagName("feature");
			WinVMJConsole.println("Print selected...");
			String[] selectedFeature = getSelectedFeature(featureList);
			WinVMJConsole.println("Done reading selected feature");
			generateMainMenu(project, sourceProduct, selectedFeature, featureMap);
//			if (!compiledProductDir.exists()) compiledProductDir.create(false, true, null);
//			importWinVMJLibraries(compiledProductDir, sourceProduct);
//			importWinVMJProductConfigs(compiledProductDir);
//			generateConfigFiles(project, sourceProduct);
//			compileModules(project, compiledProductDir, sourceProduct);
		} catch (CoreException  | SAXException e) {
			e.printStackTrace();
		}
	}
	
	private static Map<String, Object> mapFeature(NodeList featureMapList) {

		WinVMJConsole.println("Mapping feature");
		Map<String, Object> featureMap = new HashMap<String, Object>();
		
		for (int i = 0; i < featureMapList.getLength(); i++) {
			Map<String, String> mapValue = new HashMap<String, String>();
			Element feature = (Element) featureMapList.item(i);
			NamedNodeMap attributes = feature.getAttributes();
			WinVMJConsole.println("Name: " + feature.getNodeName());
			for (int j = 0; j < attributes.getLength(); j++) {
				Node attribute = attributes.item(j);
				WinVMJConsole.println(attribute.getNodeName() + ": " + attribute.getNodeValue());
				mapValue.put(attribute.getNodeName(), attribute.getNodeValue());
			}
			featureMap.put(feature.getAttribute("name"), mapValue);
		}
		
		return featureMap;
	}
	
	private static String[] getSelectedFeature(NodeList featureList) {
		int selectedCount = 0;
		for (int i = 0; i < featureList.getLength(); i++) {
			Element feature = (Element) featureList.item(i);
			if (isElementSelected(feature)) {
				selectedCount += 1;
			}
		}
		if (selectedCount == 0) {
			return null;
		}
		String[] selectedFeature = new String[selectedCount];
		selectedCount = 0;
		for (int i = 0; i < featureList.getLength(); i++) {
			Element feature = (Element) featureList.item(i);
			if (isElementSelected(feature)) {
				selectedFeature[selectedCount++] = feature.getAttribute("name");
			}
		}
		return selectedFeature;
	}
	
	private static boolean isElementSelected(Element element) {
		return element.hasAttribute("automatic") || element.hasAttribute("manual");
	}
	
	private static DocumentBuilder getDocumentBuilder() {
		if (documentBuilder == null) {
			try {
				documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			} catch (ParserConfigurationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return documentBuilder;
	}
	
	private static Element readXmlFile(String xmlFilePath) throws SAXException, IOException {
		return readXmlFile(new File(xmlFilePath));
	}
	
	private static Element readXmlFile(File file) throws SAXException, IOException {
		Document document = getDocumentBuilder().parse(file);
		document.getDocumentElement().normalize();
		return document.getDocumentElement();
	}
	
	private static List<String> parseModuleInfo(IFeatureProject project, IFolder module) 
			throws IOException, CoreException {
		IFile moduleInfo = module.getFile("module-info.java");
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
	
	private static void generateMainMenu(IFeatureProject project,
			WinVMJProduct product, String[] selectedFeature, Map<String, Object> featureMap) throws CoreException, IOException {
		WinVMJConsole.println("Generating main menu component...");
		new MenuComponentRenderer(project, selectedFeature, featureMap).render(product);
//		new HibernatePropertiesRenderer(project, dbUsername, dbPassword).render(product);
//		new RunScriptRenderer(project, dbUsername, dbPassword).render(product);
		WinVMJConsole.println("All additional config files has been generated");
	}
	
	private static void generateAppRouting(IFeatureProject project,
			WinVMJProduct product, Element[] selectedFeature, Element[] featureMap) throws CoreException, IOException {
		WinVMJConsole.println("Generating main menu component...");
//		new MenuComponentRenderer(project, selectedFeature).render(product);
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
		for (IFolder module: product.getModules()) {
			importExternalLibrariesByModuleInfo(project, externalLibraries, 
					compiledProductDir, product, module);
			compileModuleForProduct(project, compiledProductDir, module, productModule);
		}
		compileProductJar(project, compiledProductDir, productModule, product.getProductName());
		cleanBinaries(project);
	}
	
	private static List<IResource> listAllExternalLibraries(IFeatureProject project) throws CoreException {
		List<IResource> externalLibraries = new ArrayList<>();
		for (IProject externalProject: project.getProject().getReferencedProjects()) {
			CorePlugin.getDefault();
			externalLibraries.addAll(listAllExternalLibraries(
					CorePlugin.getFeatureProject(externalProject)));
		}
		externalLibraries.addAll(Arrays.asList(project.getProject()
				.getFolder(WinVMJComposer.EXTERNAL_LIB_FOLDERNAME)
				.members()));
		return externalLibraries;
	}
	
	private static void importExternalLibrariesByModuleInfo(IFeatureProject project, 
			List<IResource> externalLibs, IFolder compiledProductDir, WinVMJProduct product, 
			IFolder module) throws IOException, CoreException {
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
			IFolder module, String productModule) throws CoreException, IOException {
		IFolder compiledProductFolder = productDir.getFolder(productModule);
		IFolder binFolder = project.getProject().getFolder("bin-comp").getFolder(module.getName());
		
		List<String> compileCommand = constructCompileCommand(
				module, binFolder, compiledProductFolder);
		
		System.out.println(String.join(" ", compileCommand));

		JavaCLI.execute("Compiling " + module + " module...", 
				module + " module compiled", compileCommand);
		
		List<String> jarCommand = constructJARCommand(binFolder, compiledProductFolder, module.getName());
		
		System.out.println(String.join(" ", jarCommand));
		
		JavaCLI.execute("Packaging " + module + " module...", 
				module + " module packaged", jarCommand);
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
		
		List<String> jarCommand = constructJARCommand(binFolder, 
				compiledProductFolder, productName, mainClass);
		
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
	
	private static void transverseModuleFilePaths(IFolder submodule, 
			List<String> fileNames) throws CoreException {
		for (IResource resource: submodule.members()) {
			if (resource instanceof IFolder) 
				transverseModuleFilePaths((IFolder) resource, fileNames);
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
