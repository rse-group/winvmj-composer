package id.ac.ui.cs.prices.winvmj.composer.templates.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.core.resources.IFile;

import de.ovgu.featureide.core.IFeatureProject;
import id.ac.ui.cs.prices.winvmj.composer.monitoring.MonitoringUtils;
import id.ac.ui.cs.prices.winvmj.composer.core.WinVMJProduct;
import id.ac.ui.cs.prices.winvmj.composer.templates.TemplateRenderer;

public class LogbackXmlRenderer extends TemplateRenderer {
    private Set<String> selectedFeatures;

    public LogbackXmlRenderer(IFeatureProject project) {
        super(project);
        Set<String> features = project.loadCurrentConfiguration().getSelectedFeatureNames();
        selectedFeatures = features.stream()
            .map(name -> name.contains(".") ? name.substring(name.indexOf('.') + 1) : name)
            .collect(Collectors.toSet());
    }

    @Override
    protected IFile getOutputFile(WinVMJProduct product) {
        return project.getProject().getFolder("src-gen")
                .getFolder(product.getProductName())
                .getFile("logback.xml");
    }

    @Override
    protected Map<String, Object> extractDataModel(WinVMJProduct product) {
        Map<String, Object> dataModel = new HashMap<>();
        boolean anyLoggingEnabled = false;
        for (String featureName : MonitoringUtils.getMonitoredFeatures(selectedFeatures)) {
            if (MonitoringUtils.isLoggingEnabled(selectedFeatures, featureName)) {
                anyLoggingEnabled = true;
                break;
            }
        }
        dataModel.put("anyLoggingEnabled", anyLoggingEnabled);
        return dataModel;
    }

    @Override
    protected String loadTemplateFilename() {
        return "logback.xml";
    }
}
