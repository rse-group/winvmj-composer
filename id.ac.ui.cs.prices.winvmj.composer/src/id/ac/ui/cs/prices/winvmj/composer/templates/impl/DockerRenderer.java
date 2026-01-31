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
import id.ac.ui.cs.prices.winvmj.composer.core.WinVMJProduct;
import id.ac.ui.cs.prices.winvmj.composer.runtime.WinVMJConsole;
import id.ac.ui.cs.prices.winvmj.composer.templates.TemplateRenderer;

/**
 * Renderer for Docker-related files: Dockerfile, docker-compose.yml, .env.example
 */
public class DockerRenderer extends TemplateRenderer {

    public DockerRenderer(IFeatureProject project) {
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
        
        // Check if monitoring aspect exists
        String[] parts = product.getProductQualifiedName().split("\\.");
        String prefix = parts.length >= 1 ? parts[0] : "monitoring";
        String monitoringModuleName = prefix + ".monitoring.aspect";
        boolean hasMonitoringAspect = project.getBuildFolder().getFolder(monitoringModuleName).exists();
        dataModel.put("hasMonitoringAspect", hasMonitoringAspect);
        dataModel.put("monitoringPackage", monitoringModuleName);
        
        return dataModel;
    }

    @Override
    protected String loadTemplateFilename() {
        return "Dockerfile";
    }

    /**
     * Render all Docker files: Dockerfile, docker-compose.yml, .env.example, entrypoint.sh
     */
    public void renderAll(WinVMJProduct product) {
        // Render Dockerfile
        render(product);
        WinVMJConsole.println("[Docker] Generated Dockerfile");
        
        // Render entrypoint.sh
        renderEntrypoint(product);
        WinVMJConsole.println("[Docker] Generated entrypoint.sh");
        
        // Render docker-compose.yml
        renderDockerCompose(product);
        WinVMJConsole.println("[Docker] Generated docker-compose.yml");
        
        // Render .env.example
        renderEnvExample(product);
        WinVMJConsole.println("[Docker] Generated .env.example");
    }
    
    /**
     * Generate entrypoint.sh file
     */
    private void renderEntrypoint(WinVMJProduct product) {
        IFolder outputFolder = project.getProject().getFolder("src-gen")
                .getFolder(product.getProductName());
        IFile outputFile = outputFolder.getFile("entrypoint.sh");
        
        renderTemplate("entrypoint.sh.ftl", outputFile, extractDataModel(product));
    }

    /**
     * Generate docker-compose.yml file
     */
    private void renderDockerCompose(WinVMJProduct product) {
        IFolder outputFolder = project.getProject().getFolder("src-gen")
                .getFolder(product.getProductName());
        IFile outputFile = outputFolder.getFile("docker-compose.yml");
        
        renderTemplate("docker-compose.ftl", outputFile, extractDataModel(product));
    }

    /**
     * Generate .env.example file
     */
    private void renderEnvExample(WinVMJProduct product) {
        IFolder outputFolder = project.getProject().getFolder("src-gen")
                .getFolder(product.getProductName());
        IFile outputFile = outputFolder.getFile(".env.example");
        
        renderTemplate("env.example.ftl", outputFile, extractDataModel(product));
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
            WinVMJConsole.println("[Docker] Error rendering " + templateName + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
}
