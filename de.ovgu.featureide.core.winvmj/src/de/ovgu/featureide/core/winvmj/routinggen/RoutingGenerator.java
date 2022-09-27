package de.ovgu.featureide.core.winvmj.routinggen;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Stream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.eclipse.core.internal.resources.Folder;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
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
import de.ovgu.featureide.core.winvmj.templates.impl.RoutingComponentRenderer;
import de.ovgu.featureide.core.winvmj.templates.impl.RunScriptRenderer;
import de.ovgu.featureide.fm.core.base.IFeature;
import de.ovgu.featureide.fm.core.base.IFeatureModel;
import de.ovgu.featureide.fm.core.base.IFeatureStructure;
import de.ovgu.featureide.fm.core.configuration.Configuration;
import de.ovgu.featureide.fm.core.io.manager.IFeatureModelManager;

public class RoutingGenerator {
	
	private static String[] defaultKey = {
		"menupath",
		"menulabel",
		"routename",
		"routefilepath",
		"name"
	};
	private static DocumentBuilder documentBuilder;
	private RoutingGenerator() {};
	
	public static void generateRouting(IFeatureProject winVmjProject, IProject targetProject) throws IOException {
		try {
			Map<String, Boolean> selectedFeature = getSelectedFeature(winVmjProject);

			WinVMJConsole.println("Reading mapping...");
			Element featureMapElement = readXmlFile(winVmjProject.getProject().getFile("FeatureMapping.xml").getLocation().toString());
			Map<String, Object> featureMap = mapFeature(featureMapElement.getElementsByTagName("feature"), selectedFeature.keySet());

			Map<String, Object>[] modelStructureMap = readModelStructure(winVmjProject, featureMap);
			
			generateMainMenu(targetProject, selectedFeature, featureMap, modelStructureMap);
			generateAppRouting(targetProject, selectedFeature, featureMap);
		} catch (CoreException  | SAXException e) {
			e.printStackTrace();
		}
	}
	
	private static Map<String, Object>[] readModelStructure(IFeatureProject winVmjProject, Map<String, Object> featureMap) {
		// TODO Auto-generated method stub
		IFeatureStructure modelRoot = winVmjProject.getFeatureModel().getStructure().getRoot();
		List<IFeatureStructure> modelChildren = modelRoot.getChildren();
		Map<String, Object>[] featuresStructure = new Map[modelChildren.size()];
		for (int childIndex = 0; childIndex < featuresStructure.length; childIndex++) {
			featuresStructure[childIndex] = traverseStructureToMap(modelChildren.get(childIndex), featureMap);
		}
		return featuresStructure;
	}

	private static Map<String, Object> traverseStructureToMap(IFeatureStructure modelStructure, Map<String, Object> featureMap) {
		// TODO Auto-generated method stub
		Map<String, Object> result = new HashMap<>();
		String featureName = modelStructure.getFeature().getName().replaceAll("[- ]", "");
		result.put("name", featureName);
		boolean isAbstract = modelStructure.isAbstract();
		result.put("abstract", isAbstract);
		if (isAbstract || !featureMap.containsKey(featureName)) {
			result.put("route", "#");
			result.put("menulabel", featureName);
		} else {
			Map<String, Object> featureDetail = (Map<String, Object>) featureMap.get(featureName);
			result.put("route", featureDetail.get("menupath"));
			result.put("menulabel", featureDetail.get("menulabel"));
		}
		List<IFeatureStructure> modelChildren = modelStructure.getChildren();
		Map<String, Object>[] children = new Map[modelChildren.size()];
		for (int childIndex = 0; childIndex < children.length; childIndex++) {
			children[childIndex] = traverseStructureToMap(modelChildren.get(childIndex), featureMap);
		}
		result.put("children", children);
		return result;
	}

	private static Map<String, Object> mapFeature(NodeList featureMapList, Set<String> featureSet) {
		Map<String, Object> featureMap = new HashMap<String, Object>();
		
		for (int i = 0; i < featureMapList.getLength(); i++) {
			Map<String, String> mapValue = new HashMap<String, String>();
			Element feature = (Element) featureMapList.item(i);
			NamedNodeMap attributes = feature.getAttributes();
			for (int j = 0; j < attributes.getLength(); j++) {
				Node attribute = attributes.item(j);
				mapValue.put(attribute.getNodeName(), attribute.getNodeValue());
			}
			featureMap.put(feature.getAttribute("name"), mapValue);
		}
		
		for (String featureName : featureSet) {
			if (!featureMap.containsKey(featureName)) {
				Map<String, String> mapValue = new HashMap<String, String>();
				for (String key : defaultKey) {
					mapValue.put(key, featureName + "_" + key);
				}
				featureMap.put(featureName, mapValue);
			}
		}
		
		return featureMap;
	}
	
	private static Map<String, Boolean> getSelectedFeature(IFeatureProject winVmjProject) {
		List<IFeature> selectedFeatureList = winVmjProject.loadCurrentConfiguration().getSelectedFeatures();
		Map<String, Boolean> selectedFeatureMap = new HashMap<>();
		selectedFeatureList.stream()
				.filter(feature -> feature.getName() != "AISCO")
				.forEach(
						feature -> selectedFeatureMap.put(
								feature.getName().replaceAll("[- ]", ""), feature.getStructure().isAbstract()));
		return selectedFeatureMap;
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
	
	private static void generateMainMenu(IProject targetProject,
			Map<String, Boolean> selectedFeature, Map<String, Object> featureMap, Map<String, Object>[] modelStructureMap) throws CoreException, IOException {
		WinVMJConsole.println("Generating main menu component...");
		String[] selectedFeatureName = selectedFeature.keySet().toArray(String[]::new);
		new MenuComponentRenderer(selectedFeatureName , featureMap, modelStructureMap).render(targetProject);
		WinVMJConsole.println("Main menu component generated");
	}
	
	private static void generateAppRouting(IProject targetProject,
			Map<String, Boolean> selectedFeature, Map<String, Object> featureMap) throws CoreException, IOException {
		WinVMJConsole.println("Generating routing component...");
		List<String> selectedFeatureNonAbstractList = new ArrayList<>();
		selectedFeature.keySet().stream()
			.filter(key -> !selectedFeature.get(key))
			.forEach(key -> selectedFeatureNonAbstractList.add(key));
		new RoutingComponentRenderer(selectedFeatureNonAbstractList.toArray(String[]::new), featureMap).render(targetProject);
		WinVMJConsole.println("Routing component generated");
	}
}
