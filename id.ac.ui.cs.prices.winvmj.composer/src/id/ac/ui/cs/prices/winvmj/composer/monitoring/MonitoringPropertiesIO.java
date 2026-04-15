package id.ac.ui.cs.prices.winvmj.composer.monitoring;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.NullProgressMonitor;

/**
 * Writes monitoring configuration to .properties files in the monitoring/ folder.
 */
public class MonitoringPropertiesIO {

    private static final String MONITORING_FOLDER = "monitoring";
    private static final String[] MONITORING_TYPES = {
        "httpMetrics", "dbMetrics", "methodMetrics", "tracing", "logging"
    };

    /**
     * Save monitoring configuration to monitoring/{configName}.properties.
     * Creates the monitoring/ folder if it doesn't exist. Overwrites existing file.
     *
     * @param project       the Eclipse project
     * @param configName    config name without extension (e.g., "OverdraftDop")
     * @param jvmMetrics    whether JVM metrics is enabled
     * @param featureConfigs map of featureName -> map of monitoringType -> enabled
     */
    public static void save(IProject project, String configName,
                            boolean jvmMetrics,
                            Map<String, Map<String, Boolean>> featureConfigs) throws Exception {

        // Ensure monitoring/ folder exists
        IFolder monFolder = project.getFolder(MONITORING_FOLDER);
        if (!monFolder.exists()) {
            monFolder.create(true, true, new NullProgressMonitor());
        }

        // Build properties content
        StringBuilder sb = new StringBuilder();
        sb.append("# Monitoring configuration for ").append(configName).append("\n");
        sb.append("# Generated from configs/").append(configName).append(".xml\n\n");
        sb.append("jvmMetrics=").append(jvmMetrics).append("\n");

        for (Map.Entry<String, Map<String, Boolean>> entry : featureConfigs.entrySet()) {
            String feature = entry.getKey();
            Map<String, Boolean> types = entry.getValue();
            sb.append("\n");
            for (String type : MONITORING_TYPES) {
                boolean enabled = types.getOrDefault(type, false);
                sb.append(feature).append(".").append(type).append("=").append(enabled).append("\n");
            }
        }

        // Write file
        IFile file = monFolder.getFile(configName + ".properties");
        ByteArrayInputStream content = new ByteArrayInputStream(
                sb.toString().getBytes(StandardCharsets.UTF_8));

        if (file.exists()) {
            file.setContents(content, true, false, new NullProgressMonitor());
        } else {
            file.create(content, true, new NullProgressMonitor());
        }
    }
}
