package id.ac.ui.cs.prices.winvmj.composer.ui.wizards.pages;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Text;

import de.ovgu.featureide.core.IFeatureProject;
import id.ac.ui.cs.prices.winvmj.composer.cli.PricesDeploymentCliRunner;
import id.ac.ui.cs.prices.winvmj.composer.runtime.WinVMJConsole;
import id.ac.ui.cs.prices.winvmj.composer.ui.wizards.DeploymentWizard;

public class DeploymentExecutePage extends WizardPage {
    
    private DeploymentWizard wizard;
    private PricesDeploymentCliRunner cliRunner;
    
    private Label summaryLabel;
    private Label projectNameLabel;
    private Label targetProjectLabel;
    private Text versionText;
    private ProgressBar progressBar;
    
    public DeploymentExecutePage(DeploymentWizard wizard) {
        super("Deploy");
        setTitle("Deploy to Prices");
        setDescription("Review and confirm deployment settings.");
        this.wizard = wizard;
        this.cliRunner = wizard.getCliRunner();
    }
    
    @Override
    public void createControl(Composite parent) {
        Composite container = new Composite(parent, SWT.NONE);
        container.setLayout(new GridLayout(2, false));
        
        // Source project
        Label sourceLabel = new Label(container, SWT.NONE);
        sourceLabel.setText("Source Project:");
        projectNameLabel = new Label(container, SWT.NONE);
        projectNameLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        
        // Target project
        Label targetLabel = new Label(container, SWT.NONE);
        targetLabel.setText("Deploy To:");
        targetProjectLabel = new Label(container, SWT.NONE);
        targetProjectLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        
        // Version
        Label versionLabel = new Label(container, SWT.NONE);
        versionLabel.setText("Version (optional):");
        versionText = new Text(container, SWT.BORDER);
        versionText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        versionText.setMessage("e.g., 1.0.0 (leave empty for auto)");
        
        // Spacer
        Label spacer = new Label(container, SWT.NONE);
        GridData spacerGd = new GridData(SWT.FILL, SWT.FILL, true, true);
        spacerGd.horizontalSpan = 2;
        spacer.setLayoutData(spacerGd);
        
        // Summary
        summaryLabel = new Label(container, SWT.WRAP);
        GridData summaryGd = new GridData(SWT.FILL, SWT.TOP, true, false);
        summaryGd.horizontalSpan = 2;
        summaryLabel.setLayoutData(summaryGd);
        
        // Progress bar (hidden initially)
        progressBar = new ProgressBar(container, SWT.INDETERMINATE);
        GridData progressGd = new GridData(SWT.FILL, SWT.BOTTOM, true, false);
        progressGd.horizontalSpan = 2;
        progressBar.setLayoutData(progressGd);
        progressBar.setVisible(false);
        
        setControl(container);
        setPageComplete(true);
    }
    
    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        if (visible) {
            updateSummary();
        }
    }
    
    private void updateSummary() {
        IFeatureProject project = wizard.getFeatureProject();
        String slug = wizard.getSelectedProjectSlug();
        
        if (project != null) {
            projectNameLabel.setText(project.getProjectName());
        }
        
        if (slug != null) {
            targetProjectLabel.setText(slug);
        }
        
        summaryLabel.setText("Click 'Finish' to deploy your project to Prices platform.");
    }
    
    public void runDeployment() {
        IFeatureProject project = wizard.getFeatureProject();
        String slug = wizard.getSelectedProjectSlug();
        String version = versionText != null ? versionText.getText().trim() : "";
        
        if (project == null || slug == null) {
            WinVMJConsole.println("[DEPLOY] Error: Missing project or target slug.");
            return;
        }
        
        try {
            // Find the compiled JAR or create archive from src-gen
            Path srcGenPath = Paths.get(project.getProject().getLocation().toOSString(), "src-gen");
            
            if (!Files.exists(srcGenPath)) {
                WinVMJConsole.println("[DEPLOY] Error: src-gen folder not found. Please compile the project first.");
                return;
            }
            
            // Create a temporary zip archive of src-gen
            Path tempZip = Files.createTempFile("deploy-", ".zip");
            WinVMJConsole.println("[DEPLOY] Creating deployment archive...");
            
            createZipArchive(srcGenPath, tempZip);
            
            WinVMJConsole.println("[DEPLOY] Archive created: " + tempZip);
            WinVMJConsole.println("[DEPLOY] Deploying to project: " + slug);
            
            int exitCode;
            if (version != null && !version.isEmpty()) {
                exitCode = cliRunner.deploy(slug, tempZip, version);
            } else {
                exitCode = cliRunner.deploy(slug, tempZip);
            }
            
            if (exitCode == 0) {
                WinVMJConsole.println("[DEPLOY] Deployment completed successfully!");
            } else {
                WinVMJConsole.println("[DEPLOY] Deployment failed with exit code: " + exitCode);
            }
            
            // Cleanup temp file
            Files.deleteIfExists(tempZip);
            
        } catch (Exception e) {
            WinVMJConsole.println("[DEPLOY] Error during deployment: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void createZipArchive(Path sourceDir, Path targetZip) throws Exception {
        try (java.util.zip.ZipOutputStream zos = new java.util.zip.ZipOutputStream(
                Files.newOutputStream(targetZip))) {
            
            Files.walk(sourceDir)
                .filter(path -> !Files.isDirectory(path))
                .forEach(path -> {
                    try {
                        String entryName = sourceDir.relativize(path).toString().replace("\\", "/");
                        java.util.zip.ZipEntry entry = new java.util.zip.ZipEntry(entryName);
                        zos.putNextEntry(entry);
                        Files.copy(path, zos);
                        zos.closeEntry();
                    } catch (Exception e) {
                        WinVMJConsole.println("[DEPLOY] Error adding file to archive: " + e.getMessage());
                    }
                });
        }
    }
    
    @Override
    public boolean isPageComplete() {
        return wizard.getSelectedProjectSlug() != null;
    }
}
