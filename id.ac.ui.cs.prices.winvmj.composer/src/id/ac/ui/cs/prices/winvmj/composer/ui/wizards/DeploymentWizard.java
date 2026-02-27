package id.ac.ui.cs.prices.winvmj.composer.ui.wizards;

import org.eclipse.jface.wizard.Wizard;

import de.ovgu.featureide.core.IFeatureProject;
import id.ac.ui.cs.prices.winvmj.composer.cli.PricesDeploymentCliRunner;
import id.ac.ui.cs.prices.winvmj.composer.cli.PricesDeploymentCliRunner.CliResult;
import id.ac.ui.cs.prices.winvmj.composer.runtime.WinVMJConsole;
import id.ac.ui.cs.prices.winvmj.composer.ui.wizards.pages.DeploymentAuthPage;
import id.ac.ui.cs.prices.winvmj.composer.ui.wizards.pages.DeploymentProjectsPage;
import id.ac.ui.cs.prices.winvmj.composer.ui.wizards.pages.DeploymentExecutePage;

public class DeploymentWizard extends Wizard {
    
    private IFeatureProject project;
    private PricesDeploymentCliRunner cliRunner;
    
    private DeploymentAuthPage authPage;
    private DeploymentProjectsPage projectsPage;
    private DeploymentExecutePage executePage;
    
    private String selectedProjectSlug;
    private CliResult userInfo;
    
    public DeploymentWizard(IFeatureProject project) {
        setWindowTitle("Prices Deployment");
        setNeedsProgressMonitor(true);
        this.project = project;
        this.cliRunner = new PricesDeploymentCliRunner();
    }
    
    @Override
    public void addPages() {
        authPage = new DeploymentAuthPage(this);
        projectsPage = new DeploymentProjectsPage(this);
        executePage = new DeploymentExecutePage(this);
        
        addPage(authPage);
        addPage(projectsPage);
        addPage(executePage);
    }
    
    @Override
    public boolean performFinish() {
        WinVMJConsole.showConsole();
        WinVMJConsole.println("\n=== Prices Deployment ===");
        WinVMJConsole.println("Project: " + project.getProjectName());
        WinVMJConsole.println("Target: " + selectedProjectSlug);
        
        // Execute deployment in background thread
        new Thread(() -> {
            try {
                executePage.runDeployment();
            } catch (Exception e) {
                WinVMJConsole.println("[ERROR] Deployment failed: " + e.getMessage());
            }
        }).start();
        
        return true;
    }
    
    @Override
    public boolean canFinish() {
        return executePage != null && executePage.isPageComplete();
    }
    
    // Getters and setters
    
    public IFeatureProject getFeatureProject() {
        return project;
    }
    
    public PricesDeploymentCliRunner getCliRunner() {
        return cliRunner;
    }
    
    public String getSelectedProjectSlug() {
        return selectedProjectSlug;
    }
    
    public void setSelectedProjectSlug(String slug) {
        this.selectedProjectSlug = slug;
    }
    
    public CliResult getUserInfo() {
        return userInfo;
    }
    
    public void setUserInfo(CliResult userInfo) {
        this.userInfo = userInfo;
    }
    
    public DeploymentAuthPage getAuthPage() {
        return authPage;
    }
    
    public DeploymentProjectsPage getProjectsPage() {
        return projectsPage;
    }
    
    public DeploymentExecutePage getExecutePage() {
        return executePage;
    }
}
