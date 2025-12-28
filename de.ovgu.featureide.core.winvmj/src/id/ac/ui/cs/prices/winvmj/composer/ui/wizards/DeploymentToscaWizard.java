package id.ac.ui.cs.prices.winvmj.composer.ui.wizards;

import de.ovgu.featureide.core.IFeatureProject;
import id.ac.ui.cs.prices.winvmj.composer.runtime.WinVMJConsole;
import id.ac.ui.cs.prices.winvmj.composer.ui.handlers.DeploymentToscaHandler;
import id.ac.ui.cs.prices.winvmj.composer.ui.wizards.models.NFRDefinition.NFRDefinition;
import id.ac.ui.cs.prices.winvmj.composer.ui.wizards.pages.CredentialToscaPage;
import id.ac.ui.cs.prices.winvmj.composer.ui.wizards.pages.DeploymentInformationPage;
import id.ac.ui.cs.prices.winvmj.composer.ui.wizards.pages.InstanceRegionPage;
import id.ac.ui.cs.prices.winvmj.composer.ui.wizards.pages.NFRDefinitionPage;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Platform;
import org.osgi.framework.Bundle;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.jface.wizard.Wizard;

public class DeploymentToscaWizard extends Wizard {
    private IFeatureProject project;
    private InstanceRegionPage instanceRegionPage;
    private NFRDefinitionPage nfrDefinitionPage;
    private DeploymentInformationPage deploymentInformationPage;
    private CredentialToscaPage credentialAndProductPage;
    
    // TOSCA deployment method flag
    private boolean useToscaDeployment = true;

    public DeploymentToscaWizard(IFeatureProject project) {
        setWindowTitle("Deployment Wizard V2 - TOSCA Integration");
        this.project = project;
    }

    @Override
    public void addPages() {
        if (NFRDefinition.getRegion() == null) {
            NFRDefinition.setDefaultRegion();
        }

        instanceRegionPage = new InstanceRegionPage(project, "Instance and Region Definition");
        addPage(instanceRegionPage);

        if (NFRDefinition.getTransaction() != null || NFRDefinition.getTPS() != null) {
            fillDefaultValueForKNNIfNeeded();
            nfrDefinitionPage = new NFRDefinitionPage(project, "NFR Definition");
            addPage(nfrDefinitionPage);
        }

        deploymentInformationPage = new DeploymentInformationPage(project);
        addPage(deploymentInformationPage);

        credentialAndProductPage = new CredentialToscaPage(true, project);
        addPage(credentialAndProductPage);
    }

    @Override
    public boolean performFinish() {
        if (useToscaDeployment) {
            handleToscaDeployment();
        } else {
            handleProvisioningDeployment();
        }
        return true;
    }

    private void fillDefaultValueForKNNIfNeeded() {
        if (NFRDefinition.getTPS() == null) {
            NFRDefinition.setDefaultTPS();
        }

        if (NFRDefinition.getTransaction() == null) {
            NFRDefinition.setDefaultTransaction();
        }
    }

    /**
     * TOSCA-based deployment handler
     */
    private void handleToscaDeployment() {
        boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win");
        String provider = credentialAndProductPage.getProvider(); // AWS or GCP
        String region = credentialAndProductPage.getRegion();
        String productZipPath = isWindows ? convertWindowsPathToWslPath(project.getProject().getLocation().toFile().toPath().resolve("application.zip").toString()) : project.getProject().getLocation().toFile().toPath().resolve("application.zip").toString();
        String frontEndZipPath = isWindows ? convertWindowsPathToWslPath(credentialAndProductPage.getFrontEndPath()) : credentialAndProductPage.getFrontEndPath();
        String sshKeyPath = isWindows ? convertWindowsPathToWslPath(credentialAndProductPage.getSshKeyPath()) : credentialAndProductPage.getSshKeyPath();
        String sshKey = sshKeyPath.split("/")[sshKeyPath.split("/").length-1];
        try {
            new ProcessBuilder("wsl", "cp", sshKeyPath, "/tmp/" + sshKey).start().waitFor();
            new ProcessBuilder("wsl", "chmod", "600", "/tmp/" + sshKey).start().waitFor();
            WinVMJConsole.println("[TOSCA] SSH Key File Permission is now 600");
        } catch (Exception e) {
            WinVMJConsole.println("[ERROR] Failed to chmod ssh key file: " + e.getMessage());
        }

        String sshKeyPathModified = "/tmp/" + sshKey;

        String dbPassword = credentialAndProductPage.getDbPassword();
        String dbUsername = credentialAndProductPage.getDbUsername();
        String dbName = credentialAndProductPage.getDbName();
        
        // AWS-specific
        String awsAccessKey = credentialAndProductPage.getAwsAccessKey();
        String awsSecretKey = credentialAndProductPage.getAwsSecretKey();
        String awsSessionToken = credentialAndProductPage.getAwsSessionToken();
        String awsSshKeyName = credentialAndProductPage.getAwsSshKeyName();
        
        // GCP-specific
        String gcpServiceAccountFile = credentialAndProductPage.getGcpServiceAccountFile();
        String gcpProject = credentialAndProductPage.getGcpProject();
        String gcpZone = credentialAndProductPage.getGcpZone();
        String gcpSshUser = credentialAndProductPage.getGcpSshUser();

        // Locate TOSCA template directory
        String templateName = credentialAndProductPage.getTemplateName();
        String templateDir = credentialAndProductPage.getToscaTemplatePath();
        
        new Thread(() -> {
            try {
                WinVMJConsole.println("[TOSCA] Starting TOSCA-based deployment...");
                
                // Step 1: Create deployment inputs file
                String inputsFilePath = createDeploymentInputsFile(
                    templateName, templateDir, provider, region, sshKeyPathModified,
                    dbPassword, dbUsername, dbName,
                    awsAccessKey, awsSecretKey, awsSessionToken, awsSshKeyName,
                    gcpServiceAccountFile, gcpProject, gcpZone, gcpSshUser
                );
                
                // Step 2: Run TOSCA deployment scripts
                runToscaDeployment(
                    templateName,
                    templateDir,
                    productZipPath,
                    frontEndZipPath,
                    inputsFilePath
                );
                
            } catch (Exception e) {
                WinVMJConsole.println("[ERROR] TOSCA deployment failed: " + e.getMessage());
                e.printStackTrace();
            }
        }).start();
    }
    
    /**
     * Create deployment-inputs.ignored.yaml file
     */
    private String createDeploymentInputsFile( 
            String templateName,
            String templateDir, String provider, String region, String sshKeyPath, 
            String dbPassword, String dbUsername, String dbName,
            String awsAccessKey, String awsSecretKey, String awsSessionToken, String awsSshKeyName,
            String gcpServiceAccountFile, String gcpProject, String gcpZone, String gcpSshUser) throws Exception {
        
        File projectLocation = project.getProject().getLocation().toFile();
        File inputsFile = new File(projectLocation, "deployment-inputs.ignored.yaml");
        String inputsFilePath = inputsFile.getAbsolutePath();
        
        StringBuilder yaml = new StringBuilder();

        yaml.append("instance_name: \"").append(templateName).append("\"\n\n");
        yaml.append("product_name: \"").append(project.getCurrentConfiguration().toFile().getName().split("\\.")[0]).append("\"\n\n");
        
        yaml.append("# Database Configuration\n");
        yaml.append("db_password: \"").append(dbPassword).append("\"\n");
        yaml.append("db_username: \"").append(dbUsername).append("\"\n");
        yaml.append("db_name: \"").append(dbName).append("\"\n\n");

        yaml.append("# Frontend Configuration\n");
        yaml.append("frontend_port: 80\n");
        yaml.append("backend_port: 8080\n");
        yaml.append("backend_host: \"localhost\"\n\n");
    
        
        if ("AWS".equalsIgnoreCase(provider)) {
            yaml.append("# AWS Configuration\n");
            yaml.append("aws_access_key: \"").append(awsAccessKey).append("\"\n");
            yaml.append("aws_secret_key: \"").append(awsSecretKey).append("\"\n");
            yaml.append("aws_session_token: \"").append(awsSessionToken).append("\"\n");
            yaml.append("aws_instance_type: \"").append(NFRDefinition.getInstance().get()).append("\"\n");
            yaml.append("aws_region: \"").append(region).append("\"\n");
            yaml.append("aws_ssh_key_name: \"").append(awsSshKeyName).append("\"\n");
            yaml.append("aws_ssh_key_file: \"").append(sshKeyPath).append("\"\n");
        } else if ("GCP".equalsIgnoreCase(provider)) {
            yaml.append("# GCP Configuration\n");
            yaml.append("gcp_service_account_file: \"").append(convertWindowsPathToWslPath(gcpServiceAccountFile)).append("\"\n");
            yaml.append("gcp_project: \"").append(gcpProject).append("\"\n");
            yaml.append("gcp_instance_type: \"").append(NFRDefinition.getInstance().get()).append("\"\n");
            yaml.append("gcp_region: \"").append(region).append("\"\n");
            yaml.append("gcp_zone: \"").append(gcpZone).append("\"\n");
            yaml.append("gcp_ssh_user: \"").append(gcpSshUser).append("\"\n");
            yaml.append("gcp_ssh_key_file: \"").append(convertWindowsPathToWslPath(sshKeyPath)).append("\"\n");
        }
        
        try (FileWriter writer = new FileWriter(inputsFilePath)) {
            writer.write(yaml.toString());
        }
        
        WinVMJConsole.println("[TOSCA] Created deployment inputs file: " + inputsFilePath);
        return inputsFilePath;
    }
    
    
    /**
     * Run TOSCA deployment using Vintner
     */
    private void runToscaDeployment(String templateName, String templateDir, String sourceJarPath, String frontEndZipPath, String toscaInputPath) {
        boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win");
        String scriptsDir = templateDir + "/scripts";
        
        // Determine deployment variant based on provider
        String deploymentVariant = "AWS".equalsIgnoreCase(NFRDefinition.getProvider().get()) ? "aws_deployment" : "gcp_deployment";
        
        List<String> command = new ArrayList<>();
        
        if (isWindows) {
            WinVMJConsole.println("[TOSCA] Detected Windows OS. Using WSL.");
            String wslScriptsDir = convertWindowsPathToWslPath(scriptsDir);
            String deployScript = wslScriptsDir + "/deploy.sh";
            
            // Make script executable
            try {
                new ProcessBuilder("wsl", "chmod", "+x", deployScript).start().waitFor();
                WinVMJConsole.println("[TOSCA] Made deploy.sh executable");
            } catch (Exception e) {
                WinVMJConsole.println("[ERROR] Failed to chmod deploy.sh: " + e.getMessage());
            }
            
            command.add("wsl");
            command.add("bash");
            command.add("-l");
            command.add(deployScript);
            command.add(templateName);
            command.add(templateDir);
            command.add(deploymentVariant);
            command.add(convertWindowsPathToWslPath(sourceJarPath));
            command.add(frontEndZipPath);
            command.add(convertWindowsPathToWslPath(toscaInputPath));
            
        } else {
            WinVMJConsole.println("[TOSCA] Detected Unix-based OS. Running directly.");
            String deployScript = scriptsDir + "/deploy.sh";
            
            // Make script executable
            try {
                new ProcessBuilder("chmod", "+x", deployScript).start().waitFor();
            } catch (Exception e) {
                WinVMJConsole.println("[ERROR] Failed to chmod deploy.sh: " + e.getMessage());
            }
            
            command.add("bash");
            command.add("-l");
            command.add(deployScript);
            command.add(templateName);
            command.add(templateDir);
            command.add(deploymentVariant);
            command.add(sourceJarPath);
            command.add(frontEndZipPath);
            command.add(toscaInputPath);
        }
        
        runCommand(command, null);
    }
    

    /**
     * Original provisioning deployment
     */
    private void handleProvisioningDeployment() {
        String isProvisioning = "yes";
        String deploymentMethod = "docker";
        String provider = NFRDefinition.getProvider().get().toLowerCase();
        String credentialPath = credentialAndProductPage.getCredentialFilePath();
        String productZipPath = credentialAndProductPage.getProductFilePath();
        String username = "ubuntu";
        String machineType = NFRDefinition.getInstance().get();
        String region = NFRDefinition.getRegion().get();
        String certificateName = "HTTP_PLACEHOLDER";
        String nginxCertName = "HTTP_PLACEHOLDER";
        String instanceName = DeploymentToscaHandler.getProductName();
        String productPrefix = DeploymentToscaHandler.getProductName();
        String productName = DeploymentToscaHandler.getProductName();
        String pubKeyPath = credentialAndProductPage.getPubKeyFilePath();
        String privKeyPath = credentialAndProductPage.getPrivKeyFilePath();
        String numBackends = "1";
       
        String scriptDir = locateScriptDir();
        String finalScriptPath = scriptDir + "/wrapper.sh";
        boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win");
        
        new Thread(() -> {
            List<String> command = new ArrayList<>();
            if (isWindows) {
                WinVMJConsole.println("Detected Windows OS. Using WSL.");
                String wslScriptPath = convertWindowsPathToWslPath(finalScriptPath);
                String wslCredentialPath = convertWindowsPathToWslPath(credentialPath);
                String wslProductPath = convertWindowsPathToWslPath(productZipPath);
                String wslPubKeyPath = convertWindowsPathToWslPath(pubKeyPath);
                String wslPrivKeyPath = convertWindowsPathToWslPath(privKeyPath);
                
                String privKeyFileName = Paths.get(privKeyPath).getFileName().toString();
                String wslHomeKeyPath = "~/.ssh/" + privKeyFileName;                 
                WinVMJConsole.println("[WIZARD] Converted WSL Path: " + wslScriptPath);
            	
                try {
                    List<String> chmodCommand = List.of("wsl", "chmod", "+x", wslScriptPath);
                    new ProcessBuilder(chmodCommand).start().waitFor();
                    WinVMJConsole.println("[WIZARD] chmod +x executed on script");
                    
                    new ProcessBuilder("wsl", "mkdir", "-p", "~/.ssh").start().waitFor();
                    new ProcessBuilder("wsl", "cp", wslPrivKeyPath, wslHomeKeyPath).start().waitFor();
                    new ProcessBuilder("wsl", "chmod", "600", wslHomeKeyPath).start().waitFor();

                    WinVMJConsole.println("[WIZARD] Copied private key to ~/.ssh/ and set permission");
                } catch (Exception e) {
                    WinVMJConsole.println("[ERROR] Failed during script and key preparation: " + e.getMessage());
                }
            	
                command = generateCommandProvisionForWin(wslScriptPath, deploymentMethod, isProvisioning, username, machineType, region, wslCredentialPath, provider, instanceName, wslPubKeyPath, productName, certificateName, nginxCertName, productPrefix, wslProductPath, wslHomeKeyPath, numBackends);
            } else {
                WinVMJConsole.println("Detected Unix-based OS. Running directly.");
                command = generateCommandProvisionForLinux(finalScriptPath, deploymentMethod, isProvisioning, username, machineType, region, credentialPath, provider, instanceName, pubKeyPath, productName, certificateName, nginxCertName, productPrefix, productZipPath, privKeyPath, numBackends);
            }

            runCommand(command, null);
            
        }).start();
    }

    private static String convertWindowsPathToWslPath(String winPath) {
        String path = winPath.replace("\\", "/");
        if (path.length() > 2 && path.charAt(1) == ':') {
            char driveLetter = Character.toLowerCase(path.charAt(0));
            return "/mnt/" + driveLetter + path.substring(2);
        }
        return path;
    }

    private void runCommand(List<String> command, String workingDir) {
        try {
            WinVMJConsole.println("[WIZARD] Running command: " + command);
            ProcessBuilder builder = new ProcessBuilder(command);
            if (workingDir != null) {
                builder.directory(new File(workingDir));
            }
            builder.redirectErrorStream(true);
            Process process = builder.start();

            boolean dnsDialogShown = false;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    WinVMJConsole.println("[SCRIPT OUTPUT] " + line);
                    if (!dnsDialogShown && line.contains("Please set up the DNS A record to point")) {
                        dnsDialogShown = true;
                        handleDnsDialog(line);
                    }
                }
            }

            int exitCode = process.waitFor();
            WinVMJConsole.println("Deployment completed with exit code: " + exitCode);
        } catch (Exception e) {
            WinVMJConsole.println("[ERROR] Failed to run command: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleDnsDialog(String line) {
        Pattern dnsPattern = Pattern.compile("point (.+?) to ([\\d\\.]+)");
        Matcher matcher = dnsPattern.matcher(line);
        
        if (matcher.find()) {
            String domain = matcher.group(1).trim();
            String ip = matcher.group(2).trim();
            
            Display.getDefault().asyncExec(() -> {
                MessageDialog.openInformation(
                    Display.getDefault().getActiveShell(),
                    "DNS Configuration Required",
                    "Please set up the DNS A record:\n\nDomain: " + domain + "\nIP Address: " + ip +
                    "\n\nSet this record, wait for propagation, and then continue."
                );
            });
        }
    }

    private String locateScriptDir() {
        Bundle bundle = Platform.getBundle("de.ovgu.featureide.core.winvmj");

        URL deployURL = null;
        File deployDir = null;
        String scriptDir = "";

        try {
            deployURL = FileLocator.toFileURL(bundle.getEntry("resources/deployment"));
            WinVMJConsole.println("deployURL: " + deployURL);
            deployDir = new File(deployURL.toURI());
            scriptDir = deployDir.getAbsolutePath();
            WinVMJConsole.println("scriptDir: " + scriptDir);
        } catch (Exception e) {
            WinVMJConsole.println("[ERROR] Failed to resolve deploy directory: " + e.getMessage());
            e.printStackTrace();
        }
        return scriptDir;
    }

    private List<String> generateCommandProvisionForWin(String wslScriptPath, String deploymentMethod,
            String isProvisioning, String username, String machineType, String region, String wslCredentialPath, String provider,
            String instanceName, String wslPubKeyPath, String productName, String certificateName, String nginxCertName,
            String productPrefix, String wslProductPath, String wslPrivKeyPath, String numBackends) {
        
        List<String> command = new ArrayList<>();
        command.add("wsl");
        command.add("bash");
        command.add(wslScriptPath); 
        command.add(deploymentMethod);
        command.add(isProvisioning);
        command.add(username);
        command.add(machineType);
        command.add(region);
        command.add(wslCredentialPath);
        command.add(provider);
        command.add(instanceName);
        command.add(wslPubKeyPath);
        command.add(productName);
        command.add(certificateName);
        command.add(nginxCertName);
        command.add(productPrefix);
        command.add(wslProductPath);
        command.add(wslPrivKeyPath);
        command.add(numBackends);
        return command;
    }
    
    private List<String> generateCommandProvisionForLinux(String scriptPath, String deploymentMethod, String isProvisioning,
            String username, String machineType, String region, String credentialPath, String provider, String instanceName,
            String pubKeyPath, String productName, String certificateName, String nginxCertName,
            String productPrefix, String productPath, String privKeyPath, String numBackends) {
        List<String> command = new ArrayList<>();
        command.clear();
        command.add("bash");
        command.add(scriptPath); 
        command.add(deploymentMethod);
        command.add(isProvisioning);
        command.add(username);
        command.add(machineType);
        command.add(region);
        command.add(credentialPath);
        command.add(provider);
        command.add(instanceName);
        command.add(pubKeyPath);
        command.add(productName);
        command.add(certificateName);
        command.add(nginxCertName);
        command.add(productPrefix);
        command.add(productPath);
        command.add(privKeyPath);
        command.add(numBackends);
        return command;
    }
}