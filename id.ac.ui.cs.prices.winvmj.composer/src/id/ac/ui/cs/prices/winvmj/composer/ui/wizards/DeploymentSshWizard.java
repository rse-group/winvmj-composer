package id.ac.ui.cs.prices.winvmj.composer.ui.wizards;

import org.eclipse.jface.wizard.Wizard;

import de.ovgu.featureide.core.IFeatureProject;
import id.ac.ui.cs.prices.winvmj.composer.cli.PricesDeploymentCliRunner;
import id.ac.ui.cs.prices.winvmj.composer.ui.wizards.pages.DeploymentSshPage;

public class DeploymentSshWizard extends Wizard {
    
    private IFeatureProject project;
    private PricesDeploymentCliRunner cliRunner;
    private DeploymentSshPage sshPage;
    
    public DeploymentSshWizard(IFeatureProject project) {
        setWindowTitle("Prices SSH Deployment");
        setNeedsProgressMonitor(true);
        this.project = project;
        this.cliRunner = new PricesDeploymentCliRunner();
    }
    
    @Override
    public void addPages() {
        sshPage = new DeploymentSshPage(this);
        addPage(sshPage);
    }
    
    @Override
    public boolean performFinish() {
        return true;
    }
    
    @Override
    public boolean canFinish() {
        if (sshPage != null && sshPage.isDeploying()) {
            return false;
        }
        return sshPage != null && sshPage.isPageComplete();
    }
    
    public IFeatureProject getFeatureProject() {
        return project;
    }
    
    public PricesDeploymentCliRunner getCliRunner() {
        return cliRunner;
    }
}
