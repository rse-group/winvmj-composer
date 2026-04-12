package id.ac.ui.cs.prices.winvmj.composer.ui.wizards.pages;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Text;

import de.ovgu.featureide.core.IFeatureProject;
import id.ac.ui.cs.prices.winvmj.composer.cli.PricesDeploymentCliRunner;
import id.ac.ui.cs.prices.winvmj.composer.ui.wizards.DeploymentWizard;

public class DeploymentExecutePage extends WizardPage {
    
    private static final int STATE_IDLE = 0;
    private static final int STATE_DEPLOYING = 1;
    private static final int STATE_DONE = 2;
    
    private DeploymentWizard wizard;
    private PricesDeploymentCliRunner cliRunner;
    
    private Label targetProjectLabel;
    private Text folderPathText;
    private Button browseButton;
    private Button deployButton;
    private ProgressBar progressBar;
    private Text logText;
    
    private int deployState = STATE_IDLE;
    
    public DeploymentExecutePage(DeploymentWizard wizard) {
        super("Deploy");
        setTitle("Deploy to Prices");
        setDescription("Configure and deploy your project.");
        this.wizard = wizard;
        this.cliRunner = wizard.getCliRunner();
    }
    
    @Override
    public void createControl(Composite parent) {
        Composite container = new Composite(parent, SWT.NONE);
        container.setLayout(new GridLayout(2, false));
        
        // Target project
        Label targetLabel = new Label(container, SWT.NONE);
        targetLabel.setText("Deploy To:");
        targetProjectLabel = new Label(container, SWT.NONE);
        targetProjectLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        
        // Folder to deploy
        Label folderLabel = new Label(container, SWT.NONE);
        folderLabel.setText("Folder to Deploy:");
        
        Composite folderComp = new Composite(container, SWT.NONE);
        GridLayout folderLayout = new GridLayout(2, false);
        folderLayout.marginWidth = 0;
        folderLayout.marginHeight = 0;
        folderComp.setLayout(folderLayout);
        folderComp.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        
        folderPathText = new Text(folderComp, SWT.BORDER);
        folderPathText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        folderPathText.setMessage("Select folder to zip and deploy");
        folderPathText.addModifyListener(e -> updateDeployButtonState());
        
        browseButton = new Button(folderComp, SWT.PUSH);
        browseButton.setText("Browse...");
        browseButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                DirectoryDialog dialog = new DirectoryDialog(getShell());
                dialog.setText("Select Folder to Deploy");
                dialog.setMessage("Choose the folder containing files to deploy");
                
                IFeatureProject proj = wizard.getFeatureProject();
                if (proj != null) {
                    String projectPath = proj.getProject().getLocation().toOSString();
                    dialog.setFilterPath(projectPath);
                }
                
                String selected = dialog.open();
                if (selected != null) {
                    folderPathText.setText(selected);
                }
            }
        });
        
        // View Project Detail button
        Button projectDetailBtn = new Button(container, SWT.PUSH);
        projectDetailBtn.setText("View Project Detail");
        GridData detailBtnGd = new GridData(SWT.LEFT, SWT.CENTER, false, false);
        detailBtnGd.horizontalSpan = 2;
        projectDetailBtn.setLayoutData(detailBtnGd);
        projectDetailBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                wizard.getProjectsPage().openSelectedProjectDetail();
            }
        });
        
        // Deploy button row
        Composite buttonComp = new Composite(container, SWT.NONE);
        GridData buttonGd = new GridData(SWT.FILL, SWT.CENTER, true, false);
        buttonGd.horizontalSpan = 2;
        buttonComp.setLayoutData(buttonGd);
        buttonComp.setLayout(new GridLayout(2, false));
        
        deployButton = new Button(buttonComp, SWT.PUSH);
        deployButton.setText("Deploy");
        GridData deployBtnGd = new GridData(SWT.LEFT, SWT.CENTER, false, false);
        deployBtnGd.widthHint = 100;
        deployButton.setLayoutData(deployBtnGd);
        deployButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                startDeployment();
            }
        });
        
        // Progress bar (hidden initially)
        progressBar = new ProgressBar(buttonComp, SWT.INDETERMINATE);
        GridData progressGd = new GridData(SWT.FILL, SWT.CENTER, true, false);
        progressBar.setLayoutData(progressGd);
        progressBar.setVisible(false);
        
        // Log output area
        Label logLabel = new Label(container, SWT.NONE);
        logLabel.setText("Deployment Log:");
        GridData logLabelGd = new GridData(SWT.LEFT, SWT.CENTER, false, false);
        logLabelGd.horizontalSpan = 2;
        logLabel.setLayoutData(logLabelGd);
        
        logText = new Text(container, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL | SWT.READ_ONLY);
        GridData logGd = new GridData(SWT.FILL, SWT.FILL, true, true);
        logGd.horizontalSpan = 2;
        logGd.heightHint = 200;
        logText.setLayoutData(logGd);
        logText.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
        
        setControl(container);
        setPageComplete(true);
    }
    
    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        if (visible) {
            updateLabels();
            // Clear log and reset state when returning to this page
            if (logText != null && !logText.isDisposed()) {
                logText.setText("");
            }
            deployState = STATE_IDLE;
            if (deployButton != null && !deployButton.isDisposed()) {
                deployButton.setText("Deploy");
            }
            updateDeployButtonState();
        }
    }
    
    private void updateLabels() {
        String slug = wizard.getSelectedProjectSlug();
        
        if (slug != null) {
            targetProjectLabel.setText(slug);
        }
    }
    
    private void updateDeployButtonState() {
        if (deployButton == null || deployButton.isDisposed()) return;
        
        String folderPath = folderPathText.getText().trim();
        boolean hasPath = !folderPath.isEmpty();
        boolean notDeploying = (deployState != STATE_DEPLOYING);
        
        deployButton.setEnabled(hasPath && notDeploying);
    }
    
    private void startDeployment() {
        String slug = wizard.getSelectedProjectSlug();
        String folderPath = folderPathText.getText().trim();
        
        if (slug == null) {
            appendLog("[ERROR] No target project selected.");
            return;
        }
        
        // Change state to deploying
        deployState = STATE_DEPLOYING;
        updateUIState();
        
        appendLog("=== Starting Deployment ===");
        appendLog("Target: " + slug);
        appendLog("Folder: " + (folderPath.isEmpty() ? "(current directory)" : folderPath));
        appendLog("");
        
        Display display = Display.getDefault();
        
        // Run deployment in background thread
        new Thread(() -> {
            int exitCode = cliRunner.deploy(line -> {
                // Stream each line to the log area
                display.asyncExec(() -> {
                    if (!logText.isDisposed()) {
                        appendLog(line);
                    }
                });
            }, slug, folderPath);
            
            // Update state on completion
            display.asyncExec(() -> {
                deployState = STATE_DONE;
                updateUIState();
                
                appendLog("");
                if (exitCode == 0) {
                    appendLog("=== Deployment Completed Successfully ===");
                    appendLog("Click 'View Project Detail' to check deployment status and logs.");
                } else {
                    appendLog("=== Deployment Failed (exit code: " + exitCode + ") ===");
                    appendLog("Check 'View Project Detail' for more information.");
                }
            });
        }).start();
    }
    
    private void appendLog(String line) {
        if (logText != null && !logText.isDisposed()) {
            logText.append(line + "\n");
            // Auto-scroll to bottom
            logText.setTopIndex(logText.getLineCount() - 1);
        }
    }
    
    private void updateUIState() {
        if (deployButton == null || deployButton.isDisposed()) return;
        
        boolean deploying = (deployState == STATE_DEPLOYING);
        
        deployButton.setEnabled(!deploying);
        browseButton.setEnabled(!deploying);
        folderPathText.setEnabled(!deploying);
        progressBar.setVisible(deploying);
        
        if (deployState == STATE_DONE) {
            deployButton.setText("Deploy Again");
        }
        
        // Update wizard buttons
        getContainer().updateButtons();
    }
    
    @Override
    public boolean isPageComplete() {
        return wizard.getSelectedProjectSlug() != null;
    }
    
    @Override
    public boolean canFlipToNextPage() {
        // Disable Next button
        return false;
    }
    
    public boolean isDeploying() {
        return deployState == STATE_DEPLOYING;
    }
}
