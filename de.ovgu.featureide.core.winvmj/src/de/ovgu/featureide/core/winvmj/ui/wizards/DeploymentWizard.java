package de.ovgu.featureide.core.winvmj.ui.wizards;

import java.io.BufferedReader;

import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.Wizard;
import de.ovgu.featureide.core.winvmj.runtime.WinVMJConsole;
import de.ovgu.featureide.core.winvmj.ui.wizards.pages.*;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Platform;
import org.osgi.framework.Bundle;

public class DeploymentWizard extends Wizard {
    private SelectDeploymentPage deploymentPage;
    private CredentialAndProductPage filePage;
    private DeploymentConfigPage configPage;
    private AmanahDeploymentPage amanahPage;
    private DeploymentTargetPage deploymentTargetPage;

    public DeploymentWizard() {
        setWindowTitle("Deployment Wizard");
    }

    @Override
    public void addPages() {
    	deploymentTargetPage = new DeploymentTargetPage("Deployment Target");
    	deploymentPage = new SelectDeploymentPage("Deployment Selection");
        filePage = new CredentialAndProductPage("File Selection");
        configPage = new DeploymentConfigPage("Configuration");
        amanahPage = new AmanahDeploymentPage("Amanah Configuration");
        
        addPage(deploymentTargetPage);
        addPage(deploymentPage);
        addPage(filePage);
        addPage(configPage);
        addPage(amanahPage);
    }
    
    @Override
    public IWizardPage getNextPage(IWizardPage page) {
    	if (page == deploymentTargetPage) {
    		return deploymentPage;
    	}
    	else if (page == deploymentPage) {
            String target = deploymentTargetPage.getSelectedDeploymentTarget();
            if ("amanah".equalsIgnoreCase(target)) {
                return amanahPage;
            } else {
                return filePage;
            }
        } else if (page == filePage) {
            return configPage;
        }
        // amanahPage does not lead to any further page
        return null;
    }


    @Override
    public boolean performFinish() {
    	String selectedOption = deploymentPage.getSelectedDeploymentMethod();

        if ("amanah".equalsIgnoreCase(selectedOption)) {
            String tunnelPort = amanahPage.getTunnelPort();
            String privateKeyPath = amanahPage.getPrivateKeyPath();
            String usernameAmanah = amanahPage.getUsername();
            String productName = amanahPage.getProductName();
            String productFile = amanahPage.getProductFile();

            
            String scriptDir = locateScriptDir();

            String finalScriptPath = scriptDir + "\\deploy_amanah.bat";
            
            WinVMJConsole.println("Deployment Script: " + finalScriptPath);
            
            new Thread(() -> {
            	List<String> amanahCommand = new ArrayList<>();
            	amanahCommand.add("cmd.exe");
                amanahCommand.add("/c");
            	amanahCommand.add(finalScriptPath);
            	amanahCommand.add(privateKeyPath); 
            	amanahCommand.add(usernameAmanah);
            	amanahCommand.add(tunnelPort);
            	amanahCommand.add(productName);
            	amanahCommand.add(productFile);
            	
            	try {
            		ProcessBuilder builder = new ProcessBuilder(amanahCommand);
            		builder.redirectErrorStream(true);
                    Process process = builder.start();
                    
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            WinVMJConsole.println("[SCRIPT OUTPUT] " + line);
                        }
                    }
                    int exitCode = process.waitFor();
                    WinVMJConsole.println("Deployment completed with exit code: " + exitCode);
                    
                } catch (Exception e) {
                    WinVMJConsole.println("[ERROR] Failed to deploy: " + e.getMessage());
                }
            	
            	
            }).start();
            
            

            return true;
        }
        
        
    	
        String credentialPath = filePage.getCredentialFilePath();
        String productZipPath = filePage.getProductFilePath();
        String username = configPage.getUsername();
        String machineType = configPage.getMachineType();
        String region = configPage.getRegion();
        String certificateName = configPage.getCertificateName();
        String nginxCertName = configPage.getNginxCertificateName();
        String instanceName = configPage.getInstanceName();
        String productPrefix = configPage.getProductPrefix();
        String productName = configPage.getProductName();
        String pubKeyPath = filePage.getPubKeyFilePath();
        String privKeyPath = filePage.getPrivKeyFilePath();
        
        
        printAllSelectedOption(selectedOption,credentialPath, productZipPath, username, machineType,  region, certificateName, nginxCertName,  instanceName,  productPrefix,  productName, pubKeyPath, privKeyPath);
        
        // Temukan direktori script wrapper.sh berada
        String scriptDir = locateScriptDir();

        String finalScriptPath = scriptDir + "/wrapper.sh";
        boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win");

        new Thread(() -> {
            List<String> command = new ArrayList<>();
            if (isWindows) {
            	WinVMJConsole.println("Detected Windows OS. Using WSL.");
                String wslScriptPath = convertWindowsPathToWslPath(finalScriptPath); // ubah format path script wrapper.sh ke format sesuai linux (/mnt/.../wrapper.sh)
                String wslCredentialPath = convertWindowsPathToWslPath(credentialPath);
                String wslProductPath = convertWindowsPathToWslPath(productZipPath);
                String wslPubKeyPath = convertWindowsPathToWslPath(pubKeyPath);
                String wslPrivKeyPath = convertWindowsPathToWslPath(privKeyPath);
                
                // Ambil nama file dari path private key
                String privKeyFileName = Paths.get(privKeyPath).getFileName().toString();
                
                String wslHomeKeyPath = "~/.ssh/" + privKeyFileName;
                
                WinVMJConsole.println("[DEBUG] wsl new location: " + wslHomeKeyPath);
                
                WinVMJConsole.println("[WIZARD] Converted WSL Path: " + wslScriptPath);
            	
                // ubah permission pada wrapper.sh
            	try {
                    List<String> chmodCommand = List.of("wsl", "chmod", "+x", wslScriptPath);
                    new ProcessBuilder(chmodCommand).start().waitFor();
                    WinVMJConsole.println("[WIZARD] chmod +x executed on script");
                    
                    // Buat ~/.ssh jika belum ada
                    new ProcessBuilder("wsl", "mkdir", "-p", "~/.ssh").start().waitFor();

                    // Salin private key ke ~/.ssh/
                    new ProcessBuilder("wsl", "cp", wslPrivKeyPath, wslHomeKeyPath).start().waitFor();

                    // Ubah permission jadi 600
                    new ProcessBuilder("wsl", "chmod", "600", wslHomeKeyPath).start().waitFor();

                    WinVMJConsole.println("[WIZARD] Copied private key to ~/.ssh/ and set permission");
                } catch (Exception e) {
                    WinVMJConsole.println("[ERROR] Failed during script and key preparation: " + e.getMessage());
                }
            	
            	// NOTE: Pastikan encoding script LF (linux) tidak berubah menjadi CRLF (windows) agar bisa dieksekusi
            	command = generateCommandForWin(wslScriptPath, username, machineType, region, productName, certificateName, nginxCertName, wslCredentialPath,  selectedOption, instanceName, productPrefix, wslProductPath, wslPubKeyPath, wslHomeKeyPath);
            } else {
                WinVMJConsole.println("Detected Unix-based OS. Running directly.");
                command = generateCommandForLinux(finalScriptPath, username, machineType, region, productName, certificateName, nginxCertName, credentialPath, selectedOption, instanceName,productPrefix, productZipPath, pubKeyPath, privKeyPath);
            }

            try {
                WinVMJConsole.println("[WIZARD] Running command: " + command);
                ProcessBuilder builder = new ProcessBuilder(command);
                builder.redirectErrorStream(true);
                Process process = builder.start();
                
                boolean dnsDialogShown = false;

                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        WinVMJConsole.println("[SCRIPT OUTPUT] " + line);
                        
                        // Tampilkan pengingat untuk set up DNS record
                        if (!dnsDialogShown && line.contains("Please set up the DNS A record to point")) {
                            dnsDialogShown = true;
                            handleDnsDialog(line);
                        }

                    }
                }

                int exitCode = process.waitFor();
                WinVMJConsole.println("Deployment completed with exit code: " + exitCode);
            } catch (Exception e) {
                WinVMJConsole.println("Failed to run command: " + e.getMessage());
                e.printStackTrace();
            }
        }).start();

        return true;
    }


    
    @Override
    public boolean canFinish() {
        String method = deploymentPage.getSelectedArchitecture();
        if ("amanah".equalsIgnoreCase(method)) {
            return deploymentPage.isPageComplete() && amanahPage.isPageComplete() && deploymentTargetPage.isPageComplete();
        } else {
            return deploymentPage.isPageComplete()
            	&& deploymentTargetPage.isPageComplete()
                && filePage.isPageComplete()
                && configPage.isPageComplete();
        }
    }

    
    
    
    private static String convertWindowsPathToWslPath(String winPath) {
        String path = winPath.replace("\\", "/");
        if (path.length() > 2 && path.charAt(1) == ':') {
            char driveLetter = Character.toLowerCase(path.charAt(0));
            return "/mnt/" + driveLetter + path.substring(2);
        }
        return path;
    }
    
    private void handleDnsDialog(String line) {
    	Pattern dnsPattern = Pattern.compile("point (.+?) to ([\\d\\.]+)");
    	Matcher matcher = dnsPattern.matcher(line);
    	
    	if (matcher.find()) {String domain = matcher.group(1).trim();
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

    
    private void printAllSelectedOption(String selectedOption, 
    		String credentialPath, String productZipPath, String username, String machineType, String region,  String certificateName,
    		String nginxCertName, String instanceName, String productPrefix, String productName, String pubkey, String privkey) {
    	
    	WinVMJConsole.println("=== Deployment Wizard Configuration ===");
        WinVMJConsole.println("Selected Deployment Option: " + selectedOption);
        WinVMJConsole.println("Credential File Path: " + credentialPath);
        WinVMJConsole.println("Product ZIP File Path: " + productZipPath);
        WinVMJConsole.println("Username: " + username);
        WinVMJConsole.println("Machine Type: " + machineType);
        WinVMJConsole.println("Region: " + region);
        WinVMJConsole.println("Certificate Name: " + certificateName);
        WinVMJConsole.println("NGINX Certificate Name: " + nginxCertName);
        WinVMJConsole.println("Instance Name: " + instanceName);
        WinVMJConsole.println("Product Prefix: " + productPrefix);
        WinVMJConsole.println("Product Name: " + productName);
        WinVMJConsole.println("Public Key File Path: " + pubkey);
        WinVMJConsole.println("Private Key File Path: " + privkey);
    }
    
    
    private String locateScriptDir() {
    	Bundle bundle = Platform.getBundle("de.ovgu.featureide.core.winvmj");

        URL deployURL = null;
        File deployDir = null;
        String scriptDir = "";

        try {
            deployURL = FileLocator.toFileURL(bundle.getEntry("resources/deploy"));
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
    private List<String> generateCommandForWin(String wslScriptPath, String username, String machineType,
    		String region, String productName, String certificateName, String nginxCertName, String wslCredentialPath, 
    		String selectedOption, String instanceName, String productPrefix, String wslProductPath, String wslPubKeyPath,
    		String wslPrivKeyPath) {
    	
    	List<String> command = new ArrayList<>();
    	command.clear();
    	command.add("wsl");
    	command.add("bash");
    	command.add(wslScriptPath); 
    	command.add(username);
    	command.add(machineType);
    	command.add(region);
    	command.add(productName);
    	command.add(certificateName);
    	command.add(nginxCertName);
    	command.add(wslCredentialPath);
    	command.add(selectedOption);
    	command.add(instanceName);
    	command.add(productPrefix);
    	command.add(wslProductPath);
    	command.add(wslPubKeyPath);
    	command.add(wslPrivKeyPath);
    	return command;
    }
    
    private List<String> generateCommandForLinux(String finalScriptPath, String username, String machineType,
    		String region, String productName, String certificateName, String nginxCertName, String credentialPath, 
    		String selectedOption, String instanceName, String productPrefix, String productZipPath, String pubKeyPath,
    		String privKeyPath) {
    	
    	List<String> command = new ArrayList<>();
    	command.clear();
        command.add("bash");
        command.add(finalScriptPath);
        command.add(username);   
        command.add(machineType);
        command.add(region);
        command.add(productName);
        command.add(certificateName);
        command.add(nginxCertName);
        command.add(credentialPath);
        command.add(selectedOption);
        command.add(instanceName);
        command.add(productPrefix);
        command.add(productZipPath);
        command.add(pubKeyPath);
        command.add(privKeyPath);
    	return command;
    }
    
    
    public DeploymentTargetPage getDeploymentTargetPage() {
        return deploymentTargetPage;
    }
    
    public SelectDeploymentPage getDeploymentPage() {
        return deploymentPage;
    }


}
