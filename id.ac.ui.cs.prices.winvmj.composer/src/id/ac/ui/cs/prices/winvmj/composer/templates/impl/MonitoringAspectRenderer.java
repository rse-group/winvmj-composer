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

import com.google.gson.JsonObject;

import de.ovgu.featureide.core.IFeatureProject;
import id.ac.ui.cs.prices.winvmj.composer.Utils;
import id.ac.ui.cs.prices.winvmj.composer.core.WinVMJProduct;
import id.ac.ui.cs.prices.winvmj.composer.runtime.WinVMJConsole;
import id.ac.ui.cs.prices.winvmj.composer.templates.TemplateRenderer;

public class MonitoringAspectRenderer extends TemplateRenderer {

    private JsonObject monitoringConfig;
    private Map<String, List<String>> featureToModuleMap;
    protected List<String> selectedFeature;

    public MonitoringAspectRenderer(IFeatureProject project) {
        super(project);
        try {
            monitoringConfig = Utils.getFeatureMonitoringConfig(project.getProject());
            featureToModuleMap = Utils.getFeatureToModuleMap(project.getProject());
        } catch (CoreException e) {
            e.printStackTrace();
            monitoringConfig = new JsonObject();
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


    private String getModulePackage(WinVMJProduct product) {
        return Utils.getMonitoringModuleName(product.getProductQualifiedName());
    }

    @Override
    protected Map<String, Object> extractDataModel(WinVMJProduct product) {
        Map<String, Object> dataModel = new HashMap<>();

        String modulePackage = getModulePackage(product);
        dataModel.put("productPackage", modulePackage + ".monitoring");
        dataModel.put("modulePackage", modulePackage);
        dataModel.put("productName", product.getProductName());
        
        // Get list of monitored modules (packages to intercept)
        List<String> monitoredModules = getMonitoredModules();
        dataModel.put("monitoredModules", monitoredModules);
        
        // Check if JVM metrics are enabled (global option)
        boolean enableJvmMetrics = Utils.isJvmMetricsEnabled(project.getProject());
        dataModel.put("enableJvmMetrics", enableJvmMetrics);
        
        WinVMJConsole.println("[MonitoringAspect] Generating aspect for modules: " + monitoredModules);
        WinVMJConsole.println("[MonitoringAspect] JVM metrics enabled: " + enableJvmMetrics);

        return dataModel;
    }

    @Override
    protected String loadTemplateFilename() {
        return "MonitoringAspect";
    }

    @Override
    protected IFile getOutputFile(WinVMJProduct product) {
        String modulePackage = getModulePackage(product);
        
        // Create separate module folder for MonitoringAspect
        IFolder moduleFolder = project.getBuildFolder().getFolder(modulePackage);
        if (!moduleFolder.exists()) {
            try {
                moduleFolder.create(false, true, null);
            } catch (CoreException e) {
                e.printStackTrace();
            }
        }
        
        // Create package structure inside module folder
        IFolder packageFolder = moduleFolder;
        for (String part : modulePackage.split("\\.")) {
            packageFolder = packageFolder.getFolder(part);
            if (!packageFolder.exists()) {
                try {
                    packageFolder.create(false, true, null);
                } catch (CoreException e) {
                    e.printStackTrace();
                }
            }
        }
        
        // Create monitoring subfolder
        IFolder monitoringFolder = packageFolder.getFolder("monitoring");
        if (!monitoringFolder.exists()) {
            try {
                monitoringFolder.create(false, true, null);
            } catch (CoreException e) {
                e.printStackTrace();
            }
        }
        
        return monitoringFolder.getFile("MonitoringAspect.java");
    }

    private List<String> getMonitoredModules() {
        List<String> monitoredModules = new ArrayList<>();
        
        if (monitoringConfig == null || monitoringConfig.size() == 0 || featureToModuleMap == null) {
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
        // Render if any feature has monitoring enabled OR JVM metrics enabled
        boolean hasMonitoredModules = !getMonitoredModules().isEmpty();
        boolean jvmMetricsEnabled = Utils.isJvmMetricsEnabled(project.getProject());
        return hasMonitoredModules || jvmMetricsEnabled;
    }

    /**
     * Generate META-INF/aop.xml for AspectJ load-time weaving configuration.
     */
    public void generateAopXml(WinVMJProduct product) {
        String modulePackage = getModulePackage(product);
        IFolder moduleFolder = project.getBuildFolder().getFolder(modulePackage);
        
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
            dataModel.put("aspectPackage", modulePackage + ".monitoring");
            
            // Get monitored packages for weaving
            List<String> monitoredPackages = getMonitoredModules();
            dataModel.put("monitoredPackages", monitoredPackages);
            
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
        String modulePackage = getModulePackage(product);
        IFolder moduleFolder = project.getBuildFolder().getFolder(modulePackage);
        
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
            dataModel.put("modulePackage", modulePackage);
            dataModel.put("enableJvmMetrics", Utils.isJvmMetricsEnabled(project.getProject()));
            
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
}
