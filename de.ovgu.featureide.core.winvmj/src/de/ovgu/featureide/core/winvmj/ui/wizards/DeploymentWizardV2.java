package de.ovgu.featureide.core.winvmj.ui.wizards;

import de.ovgu.featureide.core.IFeatureProject;
import de.ovgu.featureide.core.winvmj.runtime.WinVMJConsole;
import de.ovgu.featureide.core.winvmj.ui.handlers.DeploymentV2Handler;
import de.ovgu.featureide.core.winvmj.ui.wizards.pages.CredentialAndProductPage;
import de.ovgu.featureide.core.winvmj.ui.wizards.pages.DeploymentInformationPage;
import de.ovgu.featureide.core.winvmj.ui.wizards.pages.NFRDefinitionPage;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.URL;
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

public class DeploymentWizardV2 extends Wizard {
    private IFeatureProject project;
    private NFRDefinitionPage nfrDefinitionPage;
    private DeploymentInformationPage deploymentInformationPage;
    private CredentialAndProductPage credentialAndProductPage;

    public DeploymentWizardV2(IFeatureProject project) {
        setWindowTitle("Deployment Wizard V2");
        this.project = project;
    }

    @Override
    public void addPages() {
        if (DeploymentV2Handler.getRegion() == null) {
            DeploymentV2Handler.setDefaultRegion();
        }
        if (DeploymentV2Handler.getTransaction() != null || DeploymentV2Handler.getTPS() != null) {
            fillDefaultValueForKNNIfNeeded();
            nfrDefinitionPage = new NFRDefinitionPage(project, "NFR Definition");
            addPage(nfrDefinitionPage);
        }

        deploymentInformationPage = new DeploymentInformationPage(project);
        addPage(deploymentInformationPage);

        credentialAndProductPage = new CredentialAndProductPage();
        addPage(credentialAndProductPage);
    }

    @Override
    public boolean performFinish() {
        handleProvisioningDeployment();
        return true;
    }

    private void fillDefaultValueForKNNIfNeeded() {
        if (DeploymentV2Handler.getTPS() == null) {
            DeploymentV2Handler.setDefaultTPS();
        }

        if (DeploymentV2Handler.getTransaction() == null) {
            DeploymentV2Handler.setDefaultTransaction();
        }
    }

    /**
     * All the section below is taken from Rikza's Thesis
     */
    private void handleProvisioningDeployment() {
		String isProvisioning = "yes";
    	String deploymentMethod = "docker";
    	String provider = DeploymentV2Handler.getProvider().get().toLowerCase();
    	String credentialPath = credentialAndProductPage.getCredentialFilePath();
        String productZipPath = credentialAndProductPage.getProductFilePath();
        String username = "ubuntu";
        String machineType = DeploymentV2Handler.getInstance().get();
        String region = DeploymentV2Handler.getRegion().get();
        String certificateName = "HTTP_PLACEHOLDER";
        String nginxCertName = "HTTP_PLACEHOLDER";
        String instanceName = DeploymentV2Handler.getProductName();
        String productPrefix = DeploymentV2Handler.getProductName();
        String productName = DeploymentV2Handler.getProductName();
        String pubKeyPath = credentialAndProductPage.getPubKeyFilePath();
        String privKeyPath = credentialAndProductPage.getPrivKeyFilePath();
        String numBackends = "1";
       
        
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
                WinVMJConsole.println("[WIZARD] Converted WSL Path: " + wslScriptPath);
            	
                // ubah permission pada wrapper.sh
            	try {
                    List<String> chmodCommand = List.of("wsl", "chmod", "+x", wslScriptPath);
                    new ProcessBuilder(chmodCommand).start().waitFor();
                    WinVMJConsole.println("[WIZARD] chmod +x executed on script");
                    
                    // memindahkan priv key ke wsl agar bisa ubah permission
                    new ProcessBuilder("wsl", "mkdir", "-p", "~/.ssh").start().waitFor();
                    new ProcessBuilder("wsl", "cp", wslPrivKeyPath, wslHomeKeyPath).start().waitFor();
                    new ProcessBuilder("wsl", "chmod", "600", wslHomeKeyPath).start().waitFor();

                    WinVMJConsole.println("[WIZARD] Copied private key to ~/.ssh/ and set permission");
                } catch (Exception e) {
                    WinVMJConsole.println("[ERROR] Failed during script and key preparation: " + e.getMessage());
                }
            	
            	// NOTE: Pastikan encoding script LF (linux) tidak berubah menjadi CRLF (windows) agar bisa dieksekusi
            	command = generateCommandProvisionForWin(wslScriptPath, deploymentMethod, isProvisioning, username, machineType, region, wslCredentialPath, provider, instanceName, wslPubKeyPath, productName, certificateName, nginxCertName, productPrefix, wslProductPath, wslHomeKeyPath, numBackends);
            } else {
                WinVMJConsole.println("Detected Unix-based OS. Running directly.");
                command = generateCommandProvisionForLinux(finalScriptPath, deploymentMethod, isProvisioning, username, machineType, region, credentialPath, provider, instanceName, pubKeyPath, productName, certificateName, nginxCertName, productPrefix, productZipPath, privKeyPath, numBackends);
            }

            runCommand(command);
            
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

    private void runCommand(List<String> command) {
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
    
    private List<String> generateCommandProvisionForLinux(String  scriptPath, String deploymentMethod, String isProvisioning,
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
