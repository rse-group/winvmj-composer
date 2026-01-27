package id.ac.ui.cs.prices.winvmj.composer.templates.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.runtime.CoreException;

import de.ovgu.featureide.core.IFeatureProject;
import id.ac.ui.cs.prices.winvmj.composer.Utils;
import id.ac.ui.cs.prices.winvmj.composer.core.WinVMJProduct;
import id.ac.ui.cs.prices.winvmj.composer.runtime.WinVMJConsole;
import id.ac.ui.cs.prices.winvmj.composer.templates.TemplateRenderer;

public class MonitoringAspectRenderer extends TemplateRenderer {

    private Map<String, Map<String, Object>> monitoringConfig;
    private Map<String, List<String>> featureToModuleMap;
    protected List<String> selectedFeature;

    public MonitoringAspectRenderer(IFeatureProject project) {
        super(project);
        try {
            monitoringConfig = Utils.getFeatureMonitoringConfig(project.getProject());
            featureToModuleMap = Utils.getFeatureToModuleMap(project.getProject());
        } catch (CoreException e) {
            e.printStackTrace();
            monitoringConfig = new HashMap<>();
            featureToModuleMap = new HashMap<>();
        }
        getSelectedFeature(project);
    }

    private void getSelectedFeature(IFeatureProject winVmjProject) {
        Set<String> features = winVmjProject.loadCurrentConfiguration().getSelectedFeatureNames();
        features = features.stream()
            .map(name -> name.contains(".") ? name.substring(name.indexOf('.') + 1) : name)
            .collect(Collectors.toSet());
        selectedFeature = new ArrayList<>(features);
    }

    @Override
    protected Map<String, Object> extractDataModel(WinVMJProduct product) {
        Map<String, Object> dataModel = new HashMap<>();

        dataModel.put("productPackage", product.getProductQualifiedName());
        dataModel.put("productName", product.getProductName());
        
        // Get list of monitored modules (packages to intercept)
        List<String> monitoredModules = getMonitoredModules();
        dataModel.put("monitoredModules", monitoredModules);
        
        WinVMJConsole.println("[MonitoringAspect] Generating aspect for modules: " + monitoredModules);

        return dataModel;
    }

    @Override
    protected String loadTemplateFilename() {
        return "MonitoringAspect";
    }

    @Override
    protected IFile getOutputFile(WinVMJProduct product) {
        IFolder productModuleFolder = project.getBuildFolder()
                .getFolder(product.getProductQualifiedName());
        for (String modulePath : product.getProductQualifiedName().split("\\.")) {
            productModuleFolder = productModuleFolder.getFolder(modulePath);
            if (!productModuleFolder.exists())
                try {
                    productModuleFolder.create(false, true, null);
                } catch (CoreException e) {
                    e.printStackTrace();
                }
        }
        return productModuleFolder.getFile("MonitoringAspect.java");
    }

    private List<String> getMonitoredModules() {
        List<String> monitoredModules = new ArrayList<>();
        
        if (monitoringConfig == null || monitoringConfig.isEmpty() || featureToModuleMap == null) {
            return monitoredModules;
        }

        for (String feature : selectedFeature) {
            if (Utils.isFeatureMonitoringEnabled(monitoringConfig, feature)) {
                // Get modules for this feature
                List<String> modules = featureToModuleMap.get(feature);
                if (modules != null) {
                    for (String module : modules) {
                        if (!monitoredModules.contains(module)) {
                            monitoredModules.add(module);
                        }
                    }
                }
            }
        }

        return monitoredModules;
    }

    public boolean shouldRender() {
        return !getMonitoredModules().isEmpty();
    }
}
