package id.ac.ui.cs.prices.winvmj.composer.ui.wizards;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.wizard.Wizard;

import de.ovgu.featureide.core.IFeatureProject;
import id.ac.ui.cs.prices.winvmj.composer.monitoring.MonitoringPropertiesIO;
import id.ac.ui.cs.prices.winvmj.composer.runtime.WinVMJConsole;
import id.ac.ui.cs.prices.winvmj.composer.ui.wizards.pages.MonitoringConfigPage;

/**
 * Wizard for generating monitoring .properties configuration from a feature config XML.
 */
public class MonitoringConfigWizard extends Wizard {

    private IFeatureProject project;
    private IFile configFile;
    private MonitoringConfigPage page;

    public MonitoringConfigWizard(IFeatureProject project, IFile configFile) {
        this.project = project;
        this.configFile = configFile;
        setWindowTitle("Generate Monitoring Configuration");
    }

    @Override
    public void addPages() {
        page = new MonitoringConfigPage(project, configFile);
        addPage(page);
    }

    @Override
    public boolean performFinish() {
        String configName = configFile.getName().replace(".xml", "");

        try {
            MonitoringPropertiesIO.save(
                    project.getProject(),
                    configName,
                    page.isJvmMetrics(),
                    page.getFeatureConfigs()
            );
            WinVMJConsole.println("[Monitoring] Generated monitoring/" + configName + ".properties");
            return true;
        } catch (Exception e) {
            WinVMJConsole.println("[Monitoring] Error generating config: " + e.getMessage());
            return false;
        }
    }
}
