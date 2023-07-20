package de.ovgu.featureide.core.winvmj.routinggen;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import de.ovgu.featureide.core.IFeatureProject;
import de.ovgu.featureide.core.winvmj.runtime.WinVMJConsole;
import de.ovgu.featureide.core.winvmj.templates.impl.MenuComponentRenderer;
import de.ovgu.featureide.core.winvmj.templates.impl.RoutingComponentRenderer;
import de.ovgu.featureide.core.winvmj.templates.impl.SelectedFeatureRenderer;
import de.ovgu.featureide.fm.core.base.IFeature;
import de.ovgu.featureide.fm.core.base.IFeatureStructure;

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
	
	public static void generateRouting(IFeatureProject winVmjProject, IProject targetProject, IFile mappingFile) throws IOException {
		try {
			WinVMJConsole.println("Get selected features...");
			Map<String, Boolean> selectedFeature = getSelectedFeature(winVmjProject);

			WinVMJConsole.println("Reading mapping...");
			Element featureMapElement = readXmlFile(mappingFile.getLocation().toString());
			Map<String, Object> featureMap = mapFeature(featureMapElement.getElementsByTagName("feature"), selectedFeature.keySet());

			Map<String, Object>[] modelStructureMap = readModelStructure(winVmjProject, featureMap);
			
			// Conversion from Generate Menu and Route -> Generate SelectedFeature
			// generateMainMenu(targetProject, selectedFeature, featureMap, modelStructureMap);
			// generateAppRouting(targetProject, selectedFeature, featureMap);
			generateSelectedFeature(targetProject, selectedFeature);
		} catch (SAXException e) {
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
		if (!featureMap.containsKey(featureName)) {
			result.put("route", "#");
			result.put("menulabel", featureName);
		} else {
			Map<String, Object> featureDetail = (Map<String, Object>) featureMap.get(featureName);
			result.put("route", featureDetail.get("menupath"));
			result.put("menulabel", featureDetail.get("menulabel"));
		}
		List<IFeatureStructure> modelChildren = modelStructure.getChildren();
		if (modelChildren.size() > 0) {
			Map<String, Object>[] children = new Map[modelChildren.size()];
			for (int childIndex = 0; childIndex < children.length; childIndex++) {
				children[childIndex] = traverseStructureToMap(modelChildren.get(childIndex), featureMap);
			}
			result.put("children", children);
		}
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
	
	private static void generateSelectedFeature(IProject targetProject, Map<String, Boolean> selectedFeature) {
		WinVMJConsole.println("Extracting selected feature...");
		new SelectedFeatureRenderer(selectedFeature.keySet().stream().toArray(String[]::new)).render(targetProject);
		WinVMJConsole.println("Selected feature extracted");
	}
}
