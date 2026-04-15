package id.ac.ui.cs.prices.winvmj.composer.monitoring;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.eclipse.core.resources.IFile;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Parses FeatureIDE config XML files to extract active functional features.
 */
public class FeatureConfigParser {

    /**
     * Extract active feature names from a FeatureIDE config XML.
     * Returns features that are selected (manual or automatic).
     */
    public static List<String> getActiveFunctionalFeatures(IFile configFile) throws Exception {
        List<String> features = new ArrayList<>();

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();

        try (InputStream is = configFile.getContents()) {
            Document doc = builder.parse(is);
            NodeList featureNodes = doc.getElementsByTagName("feature");

            for (int i = 0; i < featureNodes.getLength(); i++) {
                Element el = (Element) featureNodes.item(i);
                String name = el.getAttribute("name");

                boolean isSelected = "selected".equals(el.getAttribute("manual"))
                        || "selected".equals(el.getAttribute("automatic"));
                if (!isSelected) continue;

                features.add(name);
            }
        }

        return features;
    }
}
