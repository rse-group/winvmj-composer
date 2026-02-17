package id.ac.ui.cs.prices.winvmj.composer.templates.impl;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.eclipse.core.resources.IFile;

import de.ovgu.featureide.core.IFeatureProject;
import id.ac.ui.cs.prices.winvmj.composer.Utils;
import id.ac.ui.cs.prices.winvmj.composer.core.WinVMJProduct;
import id.ac.ui.cs.prices.winvmj.composer.runtime.WinVMJConsole;
import id.ac.ui.cs.prices.winvmj.composer.templates.TemplateRenderer;

public class BuildGradleRenderer extends TemplateRenderer {
    private String dbUsername;
    private String dbPassword;

    public BuildGradleRenderer(IFeatureProject project, String dbUsername, String dbPassword) {
        super(project);
        this.dbUsername = dbUsername;
        this.dbPassword = dbPassword;
    }

    protected IFile getOutputFile(WinVMJProduct product) {
        return project.getProject().getFolder("src-gen")
                .getFolder(product.getProductName())
                .getFile(loadTemplateFilename());
    }

    protected Map<String, Object> extractDataModel(WinVMJProduct product) {
        Map<String, Object> dataModel = new HashMap<>();
        List<String> dependencies = getDependencies();
        // WinVMJConsole.println("Dependencies: " + dependencies.toString());
        dataModel.put("dbname", product.getProductQualifiedName().replace(".", "_"));
        dataModel.put("product", product.getProductQualifiedName());
        dataModel.put("productName", product.getProductName());
        dataModel.put("dependencies", dependencies);
        dataModel.put("dbUsername", dbUsername);
        dataModel.put("dbPassword", dbPassword);
        dataModel.put("SQLFolder", "sql");
        
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

    protected String loadTemplateFilename() {
        return "build.gradle";
    }

    private List<String> getDependencies() {
        // Pre-compile the pattern (More efficient if called repeatedly)
        Pattern pattern = Pattern.compile("(?:(?:api|implementation|compileOnly|compileOnlyApi|runtimeOnly|testImplementation|testCompileOnly|testRuntimeOnly))\\s*(['\"])((?:[^'\"\\r\\n\\t]|\\\\[^rnt])*)\\1");
        try (BufferedReader reader = new BufferedReader(
        		new InputStreamReader(
        				project.getProject().getFile("build.gradle").getContents(), StandardCharsets.UTF_8))) {
            return reader.lines()
                .map(pattern::matcher)
                .filter(Matcher::find)
                .map(m -> m.group(0).trim())
                .collect(Collectors.toList());
        } catch (Exception e) {
            e.printStackTrace();
            return Collections.emptyList(); // Return empty list on error
        }
    }
}
