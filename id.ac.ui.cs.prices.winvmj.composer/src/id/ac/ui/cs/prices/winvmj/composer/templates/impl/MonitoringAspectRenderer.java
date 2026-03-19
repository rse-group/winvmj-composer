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

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import java.io.StringWriter;
import java.io.ByteArrayInputStream;
import java.io.IOException;

import de.ovgu.featureide.core.IFeatureProject;
import id.ac.ui.cs.prices.winvmj.composer.Utils;
import id.ac.ui.cs.prices.winvmj.composer.monitoring.MonitoringUtils;
import id.ac.ui.cs.prices.winvmj.composer.core.WinVMJProduct;
import id.ac.ui.cs.prices.winvmj.composer.runtime.WinVMJConsole;
import id.ac.ui.cs.prices.winvmj.composer.templates.TemplateRenderer;

public class MonitoringAspectRenderer extends TemplateRenderer {

    protected Set<String> selectedFeatures;
    private Map<String, List<String>> featureToModuleMap;

    public MonitoringAspectRenderer(IFeatureProject project) {
        super(project);
        loadSelectedFeatures(project);
        try {
            featureToModuleMap = Utils.getFeatureToModuleMap(project.getProject());
        } catch (CoreException e) {
            e.printStackTrace();
            featureToModuleMap = new HashMap<>();
        }
    }

    private void loadSelectedFeatures(IFeatureProject winVmjProject) {
        Set<String> features = winVmjProject.loadCurrentConfiguration().getSelectedFeatureNames();
        selectedFeatures = features.stream()
            .map(name -> name.contains(".") ? name.substring(name.indexOf('.') + 1) : name)
            .collect(Collectors.toSet());
    }


    private String getMonitoringModuleName(WinVMJProduct product) {
        return MonitoringUtils.getMonitoringModuleName(product.getProductQualifiedName());
    }

    @Override
    protected Map<String, Object> extractDataModel(WinVMJProduct product) {
        Map<String, Object> dataModel = new HashMap<>();

        String monitoringModuleName = getMonitoringModuleName(product);
        dataModel.put("monitoringPackage", monitoringModuleName);
        dataModel.put("productName", product.getProductName());
        
        // Get all monitoring info in one call
        Map<String, Object> monitoringInfos = MonitoringUtils.getMonitoringInfos(selectedFeatures);
        
        // Extract values from monitoring infos
        boolean enableJvmMetrics = (Boolean) monitoringInfos.get(MonitoringUtils.INFO_JVM_METRICS_ENABLED);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> featureConfigs = (List<Map<String, Object>>) monitoringInfos.get(MonitoringUtils.INFO_FEATURE_CONFIGS);
        
        // Enrich feature configs with module packages from feature_to_module.json
        for (Map<String, Object> config : featureConfigs) {
            String featureName = (String) config.get(MonitoringUtils.CONFIG_FEATURE_NAME);
            List<String> modulePackages = featureToModuleMap.get(featureName);
            config.put("modulePackages", modulePackages != null ? modulePackages : new ArrayList<>());
        }
        
        dataModel.put("enableJvmMetrics", enableJvmMetrics);
        dataModel.put("featureMonitoringConfigs", featureConfigs);
        
        // Filter featureToModuleMap to only include features with DB metrics enabled
        Map<String, List<String>> dbMetricsFeatureToModuleMap = new HashMap<>();
        for (Map.Entry<String, List<String>> entry : featureToModuleMap.entrySet()) {
            String featureName = entry.getKey();
            if (MonitoringUtils.isDbMetricsEnabled(selectedFeatures, featureName)) {
                dbMetricsFeatureToModuleMap.put(featureName, entry.getValue());
            }
        }
        
        // Resolve @Table(name) -> featureName mapping from model source files (DB metrics enabled only)
        Map<String, String> tableToFeatureMap = Utils.resolveTableToFeatureMap(project, dbMetricsFeatureToModuleMap);
        dataModel.put("tableToFeatureMap", tableToFeatureMap);
        
        WinVMJConsole.println("[MonitoringAspect] Generating aspect for package: " + monitoringModuleName);
        WinVMJConsole.println("[MonitoringAspect] Global JVM Metrics: " + enableJvmMetrics);
        WinVMJConsole.println("[MonitoringAspect] Feature configs count: " + featureConfigs.size());
        WinVMJConsole.println("[MonitoringAspect] Table-to-feature mappings: " + tableToFeatureMap.size());

        return dataModel;
    }

    @Override
    protected String loadTemplateFilename() {
        return "MonitoringAspect";
    }

    @Override
    protected IFile getOutputFile(WinVMJProduct product) {
        String monitoringModuleName = getMonitoringModuleName(product);
        
        // Create separate module folder for MonitoringAspect
        // e.g., build/accountpl.monitoring.aspect/
        IFolder moduleFolder = project.getBuildFolder().getFolder(monitoringModuleName);
        if (!moduleFolder.exists()) {
            try {
                moduleFolder.create(false, true, null);
            } catch (CoreException e) {
                e.printStackTrace();
            }
        }
        
        // Create package structure inside module folder
        // e.g., build/accountpl.monitoring.aspect/accountpl/monitoring/aspect/
        IFolder packageFolder = moduleFolder;
        for (String part : monitoringModuleName.split("\\.")) {
            packageFolder = packageFolder.getFolder(part);
            if (!packageFolder.exists()) {
                try {
                    packageFolder.create(false, true, null);
                } catch (CoreException e) {
                    e.printStackTrace();
                }
            }
        }
        
        return packageFolder.getFile("MonitoringAspect.java");
    }

    public boolean shouldRender() {
        // Render if Monitoring feature is selected
        return MonitoringUtils.isMonitoringEnabled(selectedFeatures);
    }

    /**
     * Generate META-INF/aop.xml for AspectJ load-time weaving configuration.
     */
    public void generateAopXml(WinVMJProduct product) {
        String monitoringModuleName = getMonitoringModuleName(product);
        IFolder moduleFolder = project.getBuildFolder().getFolder(monitoringModuleName);
        
        if (!moduleFolder.exists()) {
            try {
                moduleFolder.create(false, true, null);
            } catch (CoreException e) {
                e.printStackTrace();
                return;
            }
        }
        
        // Create META-INF folder
        IFolder metaInfFolder = moduleFolder.getFolder("META-INF");
        if (!metaInfFolder.exists()) {
            try {
                metaInfFolder.create(false, true, null);
            } catch (CoreException e) {
                e.printStackTrace();
                return;
            }
        }
        
        IFile aopXmlFile = metaInfFolder.getFile("aop.xml");
        
        try {
            Configuration cfg = new Configuration(Configuration.VERSION_2_3_31);
            cfg.setClassForTemplateLoading(this.getClass(), "/templates");
            Template template = cfg.getTemplate("aop.xml.ftl");
            
            Map<String, Object> dataModel = new HashMap<>();
            dataModel.put("aspectPackage", monitoringModuleName);
            
            // Get module packages for weaving from featureToModuleMap
            List<String> monitoredPackages = new ArrayList<>();
            Set<String> monitoredFeatures = MonitoringUtils.getMonitoredFeatures(selectedFeatures);
            for (String feature : monitoredFeatures) {
                List<String> modules = featureToModuleMap.get(feature);
                if (modules != null) {
                    for (String module : modules) {
                        if (!monitoredPackages.contains(module)) {
                            monitoredPackages.add(module);
                        }
                    }
                }
            }
            dataModel.put("monitoredPackages", monitoredPackages);
            
            // Check if any feature has DB metrics enabled
            boolean anyDbMetricsEnabled = false;
            Set<String> monFeatures = MonitoringUtils.getMonitoredFeatures(selectedFeatures);
            for (String f : monFeatures) {
                if (MonitoringUtils.isDbMetricsEnabled(selectedFeatures, f)) {
                    anyDbMetricsEnabled = true;
                    break;
                }
            }
            dataModel.put("anyDbMetricsEnabled", anyDbMetricsEnabled);
            
            StringWriter writer = new StringWriter();
            template.process(dataModel, writer);
            
            ByteArrayInputStream content = new ByteArrayInputStream(
                writer.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8)
            );
            
            if (!aopXmlFile.exists()) {
                aopXmlFile.create(content, false, null);
            } else {
                aopXmlFile.setContents(content, true, false, null);
            }
            
            WinVMJConsole.println("[MonitoringAspect] Generated META-INF/aop.xml for LTW");
        } catch (CoreException | IOException | TemplateException e) {
            e.printStackTrace();
        }
    }

    /**
     * Generate module-info.java for JPMS compatibility.
     * This is required for SourceCompiler to compile the monitoring module into a JAR.
     */
    public void generateModuleInfo(WinVMJProduct product) {
        String monitoringModuleName = getMonitoringModuleName(product);
        IFolder moduleFolder = project.getBuildFolder().getFolder(monitoringModuleName);
        
        if (!moduleFolder.exists()) {
            try {
                moduleFolder.create(false, true, null);
            } catch (CoreException e) {
                e.printStackTrace();
                return;
            }
        }
        
        IFile moduleInfoFile = moduleFolder.getFile("module-info.java");
        
        try {
            Configuration cfg = new Configuration(Configuration.VERSION_2_3_31);
            cfg.setClassForTemplateLoading(this.getClass(), "/templates");
            Template template = cfg.getTemplate("MonitoringAspectModuleInfo.ftl");
            
            Map<String, Object> dataModel = new HashMap<>();
            dataModel.put("monitoringModuleName", monitoringModuleName);
            dataModel.put("enableJvmMetrics", MonitoringUtils.isJvmMetricsEnabled(selectedFeatures));
            
            boolean anyTracingEnabled = false;
            boolean anyLoggingEnabled = false;
            for (String featureName : MonitoringUtils.getMonitoredFeatures(selectedFeatures)) {
                if (MonitoringUtils.isTracingEnabled(selectedFeatures, featureName)) anyTracingEnabled = true;
                if (MonitoringUtils.isLoggingEnabled(selectedFeatures, featureName)) anyLoggingEnabled = true;
            }
            dataModel.put("anyTracingEnabled", anyTracingEnabled);
            dataModel.put("anyLoggingEnabled", anyLoggingEnabled);
            
            StringWriter writer = new StringWriter();
            template.process(dataModel, writer);
            
            ByteArrayInputStream content = new ByteArrayInputStream(
                writer.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8)
            );
            
            if (!moduleInfoFile.exists()) {
                moduleInfoFile.create(content, false, null);
            } else {
                moduleInfoFile.setContents(content, true, false, null);
            }
            
            WinVMJConsole.println("[MonitoringAspect] Generated module-info.java");
        } catch (CoreException | IOException | TemplateException e) {
            e.printStackTrace();
        }
    }

    /**
     * Generate logback.xml for SLF4J/Logback configuration with optional OTel appender.
     */
    public void generateLogbackXml(WinVMJProduct product) {
        String monitoringModuleName = getMonitoringModuleName(product);
        IFolder moduleFolder = project.getBuildFolder().getFolder(monitoringModuleName);
        
        if (!moduleFolder.exists()) {
            try {
                moduleFolder.create(false, true, null);
            } catch (CoreException e) {
                e.printStackTrace();
                return;
            }
        }
        
        IFile logbackFile = moduleFolder.getFile("logback.xml");
        
        try {
            Configuration cfg = new Configuration(Configuration.VERSION_2_3_31);
            cfg.setClassForTemplateLoading(this.getClass(), "/templates");
            Template template = cfg.getTemplate("logback.xml.ftl");
            
            Map<String, Object> dataModel = new HashMap<>();
            boolean anyLoggingEnabled = false;
            for (String featureName : MonitoringUtils.getMonitoredFeatures(selectedFeatures)) {
                if (MonitoringUtils.isLoggingEnabled(selectedFeatures, featureName)) {
                    anyLoggingEnabled = true;
                    break;
                }
            }
            dataModel.put("anyLoggingEnabled", anyLoggingEnabled);
            
            StringWriter writer = new StringWriter();
            template.process(dataModel, writer);
            
            ByteArrayInputStream content = new ByteArrayInputStream(
                writer.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8)
            );
            
            if (!logbackFile.exists()) {
                logbackFile.create(content, false, null);
            } else {
                logbackFile.setContents(content, true, false, null);
            }
            
            WinVMJConsole.println("[MonitoringAspect] Generated logback.xml");
        } catch (CoreException | IOException | TemplateException e) {
            e.printStackTrace();
        }
    }
}
