package id.ac.ui.cs.prices.winvmj.composer.ui.wizards.pages;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;

import de.ovgu.featureide.core.IFeatureProject;
import id.ac.ui.cs.prices.winvmj.composer.monitoring.FeatureConfigParser;

/**
 * Wizard page for configuring monitoring options per active functional feature.
 */
public class MonitoringConfigPage extends WizardPage {

    private static final String[] MONITORING_TYPES = {
        "httpMetrics", "dbMetrics", "methodMetrics", "tracing", "logging"
    };
    private static final String[] MONITORING_LABELS = {
        "HTTP Metrics", "DB Metrics", "Method Metrics", "Tracing", "Logging"
    };

    private IFeatureProject project;
    private IFile configFile;

    private Button jvmMetricsCheckbox;
    private Map<String, Button[]> featureCheckboxes = new LinkedHashMap<>();
    private List<String> activeFeatures = new ArrayList<>();

    public MonitoringConfigPage(IFeatureProject project, IFile configFile) {
        super("MonitoringConfig");
        this.project = project;
        this.configFile = configFile;

        String configName = configFile.getName().replace(".xml", "");
        setTitle("Monitoring Configuration");
        setDescription("Configure monitoring for: " + configName);
    }

    @Override
    public void createControl(Composite parent) {
        Composite container = new Composite(parent, SWT.NONE);
        container.setLayout(new GridLayout(1, false));

        // Parse active features from config XML
        try {
            activeFeatures = FeatureConfigParser.getActiveFunctionalFeatures(configFile);
        } catch (Exception e) {
            setErrorMessage("Failed to parse config: " + e.getMessage());
            setControl(container);
            return;
        }

        if (activeFeatures.isEmpty()) {
            Label noFeatures = new Label(container, SWT.NONE);
            noFeatures.setText("No active functional features found in this configuration.");
            setControl(container);
            return;
        }

        // Global: JVM Metrics
        jvmMetricsCheckbox = new Button(container, SWT.CHECK);
        jvmMetricsCheckbox.setText("JVM Metrics");
        jvmMetricsCheckbox.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));

        // Separator
        Label sep = new Label(container, SWT.SEPARATOR | SWT.HORIZONTAL);
        sep.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        // Per-feature monitoring groups
        for (String feature : activeFeatures) {
            Group group = new Group(container, SWT.NONE);
            group.setText(feature);
            group.setLayout(new GridLayout(3, true));
            group.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));

            Button[] checkboxes = new Button[MONITORING_TYPES.length];
            for (int i = 0; i < MONITORING_TYPES.length; i++) {
                checkboxes[i] = new Button(group, SWT.CHECK);
                checkboxes[i].setText(MONITORING_LABELS[i]);
            }
            featureCheckboxes.put(feature, checkboxes);
        }

        setControl(container);
    }

    public boolean isJvmMetrics() {
        return jvmMetricsCheckbox != null && jvmMetricsCheckbox.getSelection();
    }

    /**
     * Returns map of featureName -> map of monitoringType -> enabled.
     */
    public Map<String, Map<String, Boolean>> getFeatureConfigs() {
        Map<String, Map<String, Boolean>> result = new LinkedHashMap<>();
        for (Map.Entry<String, Button[]> entry : featureCheckboxes.entrySet()) {
            Map<String, Boolean> types = new LinkedHashMap<>();
            Button[] checkboxes = entry.getValue();
            for (int i = 0; i < MONITORING_TYPES.length; i++) {
                types.put(MONITORING_TYPES[i], checkboxes[i].getSelection());
            }
            result.put(entry.getKey(), types);
        }
        return result;
    }
}
