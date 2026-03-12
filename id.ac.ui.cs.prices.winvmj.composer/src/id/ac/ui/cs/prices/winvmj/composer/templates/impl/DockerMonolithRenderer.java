package id.ac.ui.cs.prices.winvmj.composer.templates.impl;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.runtime.CoreException;

import de.ovgu.featureide.core.IFeatureProject;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import id.ac.ui.cs.prices.winvmj.composer.Utils;
import id.ac.ui.cs.prices.winvmj.composer.core.WinVMJProduct;
import id.ac.ui.cs.prices.winvmj.composer.runtime.WinVMJConsole;
import id.ac.ui.cs.prices.winvmj.composer.templates.TemplateRenderer;

/**
 * Renderer for Docker-related files for monolith deployment: Dockerfile, docker-compose.yml, .env.example
 */
public class DockerMonolithRenderer extends TemplateRenderer {

    public DockerMonolithRenderer(IFeatureProject project) {
        super(project);
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
        
        // Check if JVM metrics are enabled (global option)
        boolean enableJvmMetrics = Utils.isJvmMetricsEnabled(project.getProject());
        dataModel.put("enableJvmMetrics", enableJvmMetrics);
        
        // Check if any feature monitoring is enabled
        boolean hasFeatureMonitoring = Utils.hasAnyFeatureMonitoringEnabled(project.getProject());
        
        // Should include monitoring if either feature monitoring or JVM metrics enabled
        boolean shouldHaveMonitoring = hasFeatureMonitoring || enableJvmMetrics;
        dataModel.put("shouldHaveMonitoring", shouldHaveMonitoring);
        
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
        
        // Render otelCollectorConfig.yaml (only if monitoring enabled)
        Map<String, Object> dataModel = extractDataModel(product);
        boolean shouldHaveMonitoring = (boolean) dataModel.get("shouldHaveMonitoring");
        if (shouldHaveMonitoring) {
            renderOtelCollectorConfig(product);
            WinVMJConsole.println("[DockerMonolith] Generated otelCollectorConfig.yaml");
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
        IFile outputFile = outputFolder.getFile("otelCollectorConfig.yaml");
        
        renderTemplate("OtelCollectorConfig.ftl", outputFile, extractDataModel(product));
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
