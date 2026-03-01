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
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import de.ovgu.featureide.core.IFeatureProject;
import id.ac.ui.cs.prices.winvmj.composer.cli.PricesDeploymentCliRunner;
import id.ac.ui.cs.prices.winvmj.composer.runtime.WinVMJConsole;
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
    private Text folderPathText;
    
    private Button deployButton;
    private Button browseButton;
    private Label statusLabel;
    
    private boolean isDeploying = false;
    private Path selectedProjectPath = null;
    
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
        folderGroup.setText("Project Folder");
        folderGroup.setLayout(new GridLayout(3, false));
        folderGroup.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
        
        new Label(folderGroup, SWT.NONE).setText("Folder*:");
        folderPathText = new Text(folderGroup, SWT.BORDER | SWT.READ_ONLY);
        folderPathText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        folderPathText.setMessage("Select project folder...");
        
        browseButton = new Button(folderGroup, SWT.PUSH);
        browseButton.setText("Browse...");
        browseButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                DirectoryDialog dialog = new DirectoryDialog(getShell());
                dialog.setText("Select Project Folder");
                dialog.setMessage("Select the folder containing your project to deploy");
                
                // Default to feature project location
                String defaultPath = featureProject.getProject().getLocation().toFile().getAbsolutePath();
                dialog.setFilterPath(defaultPath);
                
                String selected = dialog.open();
                if (selected != null) {
                    selectedProjectPath = Paths.get(selected);
                    folderPathText.setText(selected);
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
        portGroup.setText("Listening Ports");
        portGroup.setLayout(new GridLayout(4, false));
        portGroup.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
        
        new Label(portGroup, SWT.NONE).setText("Frontend Port:");
        frontendPortText = new Text(portGroup, SWT.BORDER);
        frontendPortText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        frontendPortText.setText("80");
        
        new Label(portGroup, SWT.NONE).setText("Backend Port:");
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
        
        outputText = new Text(outputGroup, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL | SWT.READ_ONLY);
        GridData outputGd = new GridData(SWT.FILL, SWT.FILL, true, true);
        outputGd.heightHint = 200;
        outputText.setLayoutData(outputGd);
        outputText.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
        
        setControl(container);
        updatePageComplete();
        
        // Add modify listeners for validation
        sshHostText.addModifyListener(e -> updatePageComplete());
        projectNameText.addModifyListener(e -> updatePageComplete());
    }
    
    private void updatePageComplete() {
        boolean valid = !sshHostText.getText().trim().isEmpty() 
                     && !projectNameText.getText().trim().isEmpty()
                     && selectedProjectPath != null;
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
        appendOutput("Path: " + selectedProjectPath + "\n");
        if (!frontendUrl.isEmpty()) appendOutput("Frontend URL: " + frontendUrl + "\n");
        if (!backendUrl.isEmpty()) appendOutput("Backend URL: " + backendUrl + "\n");
        appendOutput("Frontend Port: " + frontendPort + "\n");
        appendOutput("Backend Port: " + backendPort + "\n");
        appendOutput("\n");
        
        final int fePort = frontendPort;
        final int bePort = backendPort;
        
        new Thread(() -> {
            WinVMJConsole.println("[SSH] Starting deployment...");
            
            int exitCode = cliRunner.deploySsh(selectedProjectPath, sshHost, projectName,
                frontendUrl.isEmpty() ? null : frontendUrl,
                backendUrl.isEmpty() ? null : backendUrl,
                fePort, bePort,
                line -> Display.getDefault().asyncExec(() -> appendOutput(line + "\n"))
            );
            
            Display.getDefault().asyncExec(() -> {
                isDeploying = false;
                updatePageComplete();
                
                if (exitCode == 0) {
                    statusLabel.setText("Deployment completed successfully!");
                    appendOutput("\n✓ Deployment completed successfully!\n");
                } else {
                    statusLabel.setText("Deployment failed (exit code: " + exitCode + ")");
                    appendOutput("\n✗ Deployment failed with exit code: " + exitCode + "\n");
                }
            });
        }).start();
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
