package id.ac.ui.cs.prices.winvmj.composer.ui.wizards.pages;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import de.ovgu.featureide.core.IFeatureProject;
import id.ac.ui.cs.prices.winvmj.composer.cli.PricesDeploymentCliRunner;
import id.ac.ui.cs.prices.winvmj.composer.ui.wizards.DeploymentSshWizard;

/**
 * Wizard page for SSH deployment configuration and execution.
 */
public class DeploymentSshPage extends WizardPage {
    
    private DeploymentSshWizard wizard;
    private PricesDeploymentCliRunner cliRunner;
    private IFeatureProject featureProject;
    
    private Text sshHostText;
    private Text projectNameText;
    private Text frontendUrlText;
    private Text backendUrlText;
    private Text frontendPortText;
    private Text backendPortText;
    private Text outputText;
    private Text frontendPathText;
    private Text backendPathText;
    
    private Button deployButton;
    private Button frontendBrowseButton;
    private Button backendBrowseButton;
    private Label statusLabel;
    
    private boolean isDeploying = false;
    private Path selectedFrontendPath = null;
    private Path selectedBackendPath = null;
    
    public DeploymentSshPage(DeploymentSshWizard wizard) {
        super("SshDeployment");
        setTitle("SSH Deployment");
        setDescription("Configure and execute SSH deployment to your server.");
        this.wizard = wizard;
        this.cliRunner = wizard.getCliRunner();
        this.featureProject = wizard.getFeatureProject();
    }
    
    @Override
    public void createControl(Composite parent) {
        Composite container = new Composite(parent, SWT.NONE);
        container.setLayout(new GridLayout(1, false));
        
        // Project folder group
        Group folderGroup = new Group(container, SWT.NONE);
        folderGroup.setText("Project Folders");
        folderGroup.setLayout(new GridLayout(3, false));
        folderGroup.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
        
        new Label(folderGroup, SWT.NONE).setText("Frontend*:");
        frontendPathText = new Text(folderGroup, SWT.BORDER | SWT.READ_ONLY);
        frontendPathText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        frontendPathText.setMessage("Select frontend folder...");
        
        frontendBrowseButton = new Button(folderGroup, SWT.PUSH);
        frontendBrowseButton.setText("Browse...");
        frontendBrowseButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                DirectoryDialog dialog = new DirectoryDialog(getShell());
                dialog.setText("Select Frontend Folder");
                dialog.setMessage("Select the frontend directory to deploy");
                
                String defaultPath = featureProject.getProject().getLocation().toFile().getAbsolutePath();
                dialog.setFilterPath(defaultPath);
                
                String selected = dialog.open();
                if (selected != null) {
                    selectedFrontendPath = Paths.get(selected);
                    frontendPathText.setText(selected);
                    updatePageComplete();
                }
            }
        });
        
        new Label(folderGroup, SWT.NONE).setText("Backend*:");
        backendPathText = new Text(folderGroup, SWT.BORDER | SWT.READ_ONLY);
        backendPathText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        backendPathText.setMessage("Select backend folder...");
        
        backendBrowseButton = new Button(folderGroup, SWT.PUSH);
        backendBrowseButton.setText("Browse...");
        backendBrowseButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                DirectoryDialog dialog = new DirectoryDialog(getShell());
                dialog.setText("Select Backend Folder");
                dialog.setMessage("Select the backend directory to deploy");
                
                String defaultPath = featureProject.getProject().getLocation().toFile().getAbsolutePath();
                dialog.setFilterPath(defaultPath);
                
                String selected = dialog.open();
                if (selected != null) {
                    selectedBackendPath = Paths.get(selected);
                    backendPathText.setText(selected);
                    updatePageComplete();
                }
            }
        });
        
        // Configuration group
        Group configGroup = new Group(container, SWT.NONE);
        configGroup.setText("SSH Configuration");
        configGroup.setLayout(new GridLayout(2, false));
        configGroup.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
        
        // SSH Host
        new Label(configGroup, SWT.NONE).setText("SSH Host*:");
        sshHostText = new Text(configGroup, SWT.BORDER);
        sshHostText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        sshHostText.setMessage("e.g., amanah-node");
        
        // SSH Host info
        Label sshHostInfo = new Label(configGroup, SWT.NONE);
        sshHostInfo.setText("Use a host name defined in your ~/.ssh/config");
        GridData infoGd = new GridData(SWT.FILL, SWT.TOP, true, false);
        infoGd.horizontalSpan = 2;
        sshHostInfo.setLayoutData(infoGd);
        
        // Project Name
        new Label(configGroup, SWT.NONE).setText("Project Name*:");
        projectNameText = new Text(configGroup, SWT.BORDER);
        projectNameText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        projectNameText.setText(featureProject.getProjectName());
        
        // Custom URLs group
        Group urlGroup = new Group(container, SWT.NONE);
        urlGroup.setText("Custom URLs (Optional)");
        urlGroup.setLayout(new GridLayout(2, false));
        urlGroup.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
        
        new Label(urlGroup, SWT.NONE).setText("Frontend URL:");
        frontendUrlText = new Text(urlGroup, SWT.BORDER);
        frontendUrlText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        frontendUrlText.setMessage("e.g., https://myapp.example.com");
        
        new Label(urlGroup, SWT.NONE).setText("Backend URL:");
        backendUrlText = new Text(urlGroup, SWT.BORDER);
        backendUrlText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        backendUrlText.setMessage("e.g., https://api.myapp.example.com");
        
        // Ports group
        Group portGroup = new Group(container, SWT.NONE);
        portGroup.setText("Internal Listening Ports");
        portGroup.setLayout(new GridLayout(2, false));
        portGroup.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
        
        new Label(portGroup, SWT.NONE).setText("Frontend Internal Port:");
        frontendPortText = new Text(portGroup, SWT.BORDER);
        frontendPortText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        frontendPortText.setText("80");
        
        new Label(portGroup, SWT.NONE).setText("Backend Internal Port:");
        backendPortText = new Text(portGroup, SWT.BORDER);
        backendPortText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        backendPortText.setText("7776");
        
        // Button bar
        Composite buttonBar = new Composite(container, SWT.NONE);
        buttonBar.setLayout(new GridLayout(2, false));
        buttonBar.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
        
        deployButton = new Button(buttonBar, SWT.PUSH);
        deployButton.setText("Deploy Now");
        deployButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                executeDeploy();
            }
        });
        
        statusLabel = new Label(buttonBar, SWT.NONE);
        statusLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        statusLabel.setText("");
        
        // Output area
        Group outputGroup = new Group(container, SWT.NONE);
        outputGroup.setText("Output");
        outputGroup.setLayout(new GridLayout(1, false));
        outputGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        
        // Terminal-style output with keyboard input support
        outputText = new Text(outputGroup, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
        GridData outputGd = new GridData(SWT.FILL, SWT.FILL, true, true);
        outputGd.heightHint = 200;
        outputText.setLayoutData(outputGd);
        outputText.setEditable(false);
        
        setControl(container);
        updatePageComplete();
        
        // Add modify listeners for validation
        sshHostText.addModifyListener(e -> updatePageComplete());
        projectNameText.addModifyListener(e -> updatePageComplete());
    }
    
    private void updatePageComplete() {
        boolean valid = !sshHostText.getText().trim().isEmpty() 
                     && !projectNameText.getText().trim().isEmpty()
                     && selectedFrontendPath != null
                     && selectedBackendPath != null;
        setPageComplete(valid && !isDeploying);
        deployButton.setEnabled(valid && !isDeploying);
    }
    
    private void executeDeploy() {
        String sshHost = sshHostText.getText().trim();
        String projectName = projectNameText.getText().trim();
        String frontendUrl = frontendUrlText.getText().trim();
        String backendUrl = backendUrlText.getText().trim();
        
        int frontendPort = 80;
        int backendPort = 7776;
        
        try {
            frontendPort = Integer.parseInt(frontendPortText.getText().trim());
        } catch (NumberFormatException e) {
            // use default
        }
        
        try {
            backendPort = Integer.parseInt(backendPortText.getText().trim());
        } catch (NumberFormatException e) {
            // use default
        }
        
        isDeploying = true;
        updatePageComplete();
        outputText.setText("");
        
        statusLabel.setText("Deployment in progress...");
        appendOutput("=== SSH DEPLOYMENT ===\n");
        appendOutput("SSH Host: " + sshHost + "\n");
        appendOutput("Project: " + projectName + "\n");
        appendOutput("Frontend: " + selectedFrontendPath + "\n");
        appendOutput("Backend: " + selectedBackendPath + "\n");
        if (!frontendUrl.isEmpty()) appendOutput("Frontend URL: " + frontendUrl + "\n");
        if (!backendUrl.isEmpty()) appendOutput("Backend URL: " + backendUrl + "\n");
        appendOutput("Frontend Port: " + frontendPort + "\n");
        appendOutput("Backend Port: " + backendPort + "\n");
        appendOutput("\n");
        
        final int fePort = frontendPort;
        final int bePort = backendPort;
        
        try {
            // Launch deployment in external terminal window
            cliRunner.launchSshDeployInTerminal(
                selectedFrontendPath.toString(),
                selectedBackendPath.toString(),
                sshHost,
                projectName,
                frontendUrl.isEmpty() ? null : frontendUrl,
                backendUrl.isEmpty() ? null : backendUrl,
                fePort,
                bePort
            );
            
            appendOutput("\n✓ Deployment launched in external terminal!\n");
            appendOutput("A Command Prompt window has been opened.\n");
            appendOutput("Please complete the SSH passphrase prompts there.\n");
            statusLabel.setText("Deployment launched in external terminal");
            isDeploying = false;
            updatePageComplete();
            
        } catch (Exception e) {
            appendOutput("\n✗ Failed to launch deployment: " + e.getMessage() + "\n");
            statusLabel.setText("Failed to launch deployment");
            isDeploying = false;
            updatePageComplete();
        }
    }
    
    private void appendOutput(String text) {
        if (!outputText.isDisposed()) {
            outputText.append(text);
            outputText.setTopIndex(outputText.getLineCount() - 1);
        }
    }
    
    public boolean isDeploying() {
        return isDeploying;
    }
    
}
