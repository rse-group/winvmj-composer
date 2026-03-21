package id.ac.ui.cs.prices.winvmj.composer.templates.impl;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.runtime.CoreException;

import de.ovgu.featureide.core.IFeatureProject;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import id.ac.ui.cs.prices.winvmj.composer.core.WinVMJProduct;
import id.ac.ui.cs.prices.winvmj.composer.monitoring.MonitoringUtils;
import id.ac.ui.cs.prices.winvmj.composer.runtime.WinVMJConsole;
import id.ac.ui.cs.prices.winvmj.composer.templates.TemplateRenderer;

/**
 * Renderer for Docker-related files for monolith deployment: Dockerfile, docker-compose.yml, .env.example
 */
public class DockerMonolithRenderer extends TemplateRenderer {

    private Set<String> selectedFeatures;

    public DockerMonolithRenderer(IFeatureProject project) {
        super(project);
        loadSelectedFeatures();
    }

    private void loadSelectedFeatures() {
        Set<String> features = project.loadCurrentConfiguration().getSelectedFeatureNames();
        selectedFeatures = features.stream()
            .map(name -> name.contains(".") ? name.substring(name.indexOf('.') + 1) : name)
            .collect(Collectors.toSet());
    }

    @Override
    protected IFile getOutputFile(WinVMJProduct product) {
        // Default output file (Dockerfile)
        return project.getProject().getFolder("src-gen")
                .getFolder(product.getProductName())
                .getFile("Dockerfile");
    }

    @Override
    protected Map<String, Object> extractDataModel(WinVMJProduct product) {
        Map<String, Object> dataModel = new HashMap<>();
        
        dataModel.put("productName", product.getProductName());
        dataModel.put("productPackage", product.getProductQualifiedName());
        
        // Monitoring flags from selected features
        boolean monitoringEnabled = MonitoringUtils.isMonitoringEnabled(selectedFeatures);
        boolean enableJvmMetrics = MonitoringUtils.isJvmMetricsEnabled(selectedFeatures);
        
        // Per-feature monitoring checks
        Set<String> monitoredFeatures = MonitoringUtils.getMonitoredFeatures(selectedFeatures);
        
        boolean shouldHaveMonitoring = monitoringEnabled && (enableJvmMetrics || !monitoredFeatures.isEmpty());
        dataModel.put("shouldHaveMonitoring", shouldHaveMonitoring);
        dataModel.put("monitoringMode", MonitoringUtils.MONITORING_MODE.name());
        dataModel.put("enableJvmMetrics", enableJvmMetrics);
        
        return dataModel;
    }

    @Override
    protected String loadTemplateFilename() {
        return "DockerfileMonolith";
    }

    /**
     * Render all Docker files: Dockerfile, docker-compose.yml, .env.example, otelCollectorConfig.yaml
     */
    public void renderAll(WinVMJProduct product) {
        // Render Dockerfile
        render(product);
        WinVMJConsole.println("[DockerMonolith] Generated Dockerfile");
        
        // Render docker-compose.yml
        renderDockerCompose(product);
        WinVMJConsole.println("[DockerMonolith] Generated docker-compose.yml");
        
        // Render .env.example
        renderEnvExample(product);
        WinVMJConsole.println("[DockerMonolith] Generated .env.example");
        
        // Render observability configs (only if monitoring enabled)
        Map<String, Object> dataModel = extractDataModel(product);
        boolean shouldHaveMonitoring = (boolean) dataModel.get("shouldHaveMonitoring");
        if (shouldHaveMonitoring) {
            renderOtelCollectorConfig(product);
            WinVMJConsole.println("[DockerMonolith] Generated otel collector config");
            
            renderObservabilityConfig(product, "PrometheusConfig.ftl", "prometheus.yml");
            WinVMJConsole.println("[DockerMonolith] Generated prometheus config");
            
            renderObservabilityConfig(product, "LokiConfig.ftl", "loki-config.yaml");
            WinVMJConsole.println("[DockerMonolith] Generated loki config");
            
            renderObservabilityConfig(product, "TempoConfig.ftl", "tempo-config.yaml");
            WinVMJConsole.println("[DockerMonolith] Generated tempo config");
            
            renderObservabilityConfig(product, "GrafanaDatasources.ftl", "grafana-datasources.yaml");
            WinVMJConsole.println("[DockerMonolith] Generated grafana datasources");
            
            renderObservabilityConfig(product, "GrafanaDashboardProvisioning.ftl", "grafana-dashboards.yaml");
            WinVMJConsole.println("[DockerMonolith] Generated grafana dashboard provisioning");
            
            renderObservabilityConfig(product, "GrafanaDashboard.json.ftl", "grafana-dashboard.json");
            WinVMJConsole.println("[DockerMonolith] Generated grafana dashboard");
        }
    }
    
    /**
     * Generate docker-compose.yml file
     */
    private void renderDockerCompose(WinVMJProduct product) {
        IFolder outputFolder = project.getProject().getFolder("src-gen")
                .getFolder(product.getProductName());
        IFile outputFile = outputFolder.getFile("docker-compose.yml");
        
        renderTemplate("docker-composeMonolith.ftl", outputFile, extractDataModel(product));
    }

    /**
     * Generate .env.example file
     */
    private void renderEnvExample(WinVMJProduct product) {
        IFolder outputFolder = project.getProject().getFolder("src-gen")
                .getFolder(product.getProductName());
        IFile outputFile = outputFolder.getFile(".env.example");
        
        renderTemplate("env.exampleMonolith.ftl", outputFile, extractDataModel(product));
    }

    /**
     * Generate otelCollectorConfig.yaml file
     */
    private void renderOtelCollectorConfig(WinVMJProduct product) {
        IFolder outputFolder = project.getProject().getFolder("src-gen")
                .getFolder(product.getProductName());
        IFile outputFile = outputFolder.getFile("otel-collector-config.yaml");
        
        renderTemplate("OtelCollectorConfig.ftl", outputFile, extractDataModel(product));
    }

    /**
     * Generate an observability config file in the product output folder.
     */
    private void renderObservabilityConfig(WinVMJProduct product, String templateName, String outputFileName) {
        IFolder outputFolder = project.getProject().getFolder("src-gen")
                .getFolder(product.getProductName());
        IFile outputFile = outputFolder.getFile(outputFileName);
        renderTemplate(templateName, outputFile, extractDataModel(product));
    }

    /**
     * Helper method to render a template to a file
     */
    private void renderTemplate(String templateName, IFile outputFile, Map<String, Object> dataModel) {
        try {
            Configuration cfg = new Configuration(Configuration.VERSION_2_3_31);
            cfg.setClassForTemplateLoading(this.getClass(), "/templates");
            Template template = cfg.getTemplate(templateName);
            
            StringWriter writer = new StringWriter();
            template.process(dataModel, writer);
            
            ByteArrayInputStream content = new ByteArrayInputStream(
                writer.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8)
            );
            
            if (!outputFile.exists()) {
                outputFile.create(content, false, null);
            } else {
                outputFile.setContents(content, true, false, null);
            }
        } catch (CoreException | IOException | TemplateException e) {
            WinVMJConsole.println("[DockerMonolith] Error rendering " + templateName + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
}
