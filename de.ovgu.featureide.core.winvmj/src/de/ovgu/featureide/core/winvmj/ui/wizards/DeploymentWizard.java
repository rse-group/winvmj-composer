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
import java.nio.file.StandardCopyOption;
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
import java.util.zip.ZipOutputStream;
import java.util.zip.ZipEntry;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;


public class DeploymentWizard extends Wizard {
    private SelectDeploymentPage deploymentPage;
    private CredentialAndProductPage filePage;
    private DeploymentConfigProvisioningPage configPage;
    private DeploymentConfigExistingPage configExistPage;
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
        configPage = new DeploymentConfigProvisioningPage("Configuration");
        configExistPage = new DeploymentConfigExistingPage("Configuration");
        amanahPage = new AmanahDeploymentPage("Amanah Configuration");
        
        addPage(deploymentTargetPage);
        addPage(deploymentPage);
        addPage(filePage);
        addPage(configPage);
        addPage(configExistPage);
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
        } 
    	else if (page == filePage) {
    		String target = deploymentTargetPage.getSelectedDeploymentTarget();
    		if("provisioning".equalsIgnoreCase(target)) {
    			return configPage;
    		} else {
    			return configExistPage;
    		}

        }
        // amanahPage does not lead to any further page
        return null;
    }
    
    @Override
    public boolean performFinish() {
        String selectedTarget = deploymentTargetPage.getSelectedDeploymentTarget();

        switch (selectedTarget.toLowerCase()) {
            case "amanah":
                handleAmanahDeployment();
                break;
            case "provisioning":
                handleProvisioningDeployment();
                break;
            case "existing":
                handleExistingDeployment();
                break;
            default:
                WinVMJConsole.println("[ERROR] : No Valid Deployment Target");
                return false;
        }
        return true;
    }


	@Override
    public boolean canFinish() {
        String target = deploymentTargetPage.getSelectedDeploymentTarget();
        if ("amanah".equalsIgnoreCase(target)) {
            return deploymentPage.isPageComplete() && amanahPage.isPageComplete() && deploymentTargetPage.isPageComplete();
        } else if ("existing".equalsIgnoreCase(target)) {
        	return deploymentPage.isPageComplete()
        	&& deploymentTargetPage.isPageComplete()
            && filePage.isPageComplete()
            && configExistPage.isPageComplete();
        }
        else {
            return deploymentPage.isPageComplete()
            	&& deploymentTargetPage.isPageComplete()
                && filePage.isPageComplete()
                && configPage.isPageComplete();
        }
    }
	
	private void handleAmanahDeployment() {
	    String deploymentMethod = deploymentPage.getSelectedDeploymentMethod();
	    String tunnelPort = amanahPage.getTunnelPort();
	    String privateKeyPath = amanahPage.getPrivateKeyPath();
	    String usernameAmanah = amanahPage.getUsername();
	    String productName = amanahPage.getProductName();
	    String productFile = amanahPage.getProductFile();
	    String productPrefix = amanahPage.getProductPrefix();
	    String numBackend = amanahPage.getNumBackends();

	    String scriptPath = locateScriptDir() + "\\amanah\\" +
	            ("docker".equalsIgnoreCase(deploymentMethod) ? "deploy_amanah_docker.bat" : "deploy_amanah.bat");

	    WinVMJConsole.println("Deployment Script: " + scriptPath);

	    new Thread(() -> {
	        List<String> cmd = List.of("cmd.exe", "/c", scriptPath, privateKeyPath,
	                usernameAmanah, tunnelPort, productName, productFile, productPrefix, numBackend);

	        runCommand(cmd);
	    }).start();
	}
	
	private void handleProvisioningDeployment() {
		String isProvisioning = "yes";
    	String deploymentMethod = deploymentPage.getSelectedDeploymentMethod().toLowerCase();
    	String provider = deploymentPage.getSelectedProvider().toLowerCase();
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
        String numBackends = configPage.getNumBackends();
        
        // Periksa keberadaan zip library
        if (deploymentMethod.equalsIgnoreCase("systemd")) {
        	WinVMJConsole.println("Using Systemd Detected, Preparing winvmj libraries....");
        	zipAndCopyLib();
        }
       
        
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
	
	private void handleExistingDeployment() {
		String isProvisioning = "no";
    	String deploymentMethod = deploymentPage.getSelectedDeploymentMethod().toLowerCase();
        String productZipPath = filePage.getProductFilePath();
        String username = configExistPage.getUsername();
        String ipAddress = configExistPage.getInstanceIP();
        String certificateName = configExistPage.getCertificateName();
        String nginxCertName = configExistPage.getNginxCertificateName();
        String productPrefix = configExistPage.getProductPrefix();
        String productName = configExistPage.getProductName();
        String privKeyPath = filePage.getPrivKeyFilePath();
        String numBackends = configExistPage.getNumBackends();
        
     // Periksa keberadaan zip library
        if (deploymentMethod.equalsIgnoreCase("systemd")) {
        	WinVMJConsole.println("Using Systemd Detected, Preparing winvmj libraries....");
        	zipAndCopyLib();
        }
       
        // Temukan direktori script wrapper.sh berada
        String scriptDir = locateScriptDir();

        String finalScriptPath = scriptDir + "/wrapper.sh";
        boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win");
        
        new Thread(() -> {
            List<String> command = new ArrayList<>();
            if (isWindows) {
            	WinVMJConsole.println("Detected Windows OS. Using WSL.");
                String wslScriptPath = convertWindowsPathToWslPath(finalScriptPath); // ubah format path script wrapper.sh ke format sesuai linux (/mnt/.../wrapper.sh)
                String wslProductPath = convertWindowsPathToWslPath(productZipPath);
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
            	command = generateCommandExistingForWin(wslScriptPath, deploymentMethod, isProvisioning, username, ipAddress, productName, certificateName, nginxCertName, productPrefix, wslProductPath, wslHomeKeyPath, numBackends);
            } else {
                WinVMJConsole.println("Detected Unix-based OS. Running directly.");
                command = generateCommandExistingForLinux(finalScriptPath, deploymentMethod, isProvisioning, username, ipAddress, productName, certificateName, nginxCertName, productPrefix, productZipPath, privKeyPath, numBackends);
            }

            runCommand(command);
            
        }).start();
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
    
    private List<String> generateCommandExistingForLinux(String finalScriptPath, String deploymentMethod,
			String isProvisioning, String username, String ipAddress, String productName, String certificateName,
			String nginxCertName, String productPrefix, String productZipPath, String privKeyPath, String numBackends) {
    	List<String> command = new ArrayList<>();
	    command.clear();
        command.add("bash");
	    command.add(deploymentMethod);
	    command.add(isProvisioning);
	    command.add(username);
	    command.add(ipAddress);
	    command.add(productName);
	    command.add(certificateName);
	    command.add(nginxCertName);
	    command.add(productPrefix);
	    command.add(productZipPath);
	    command.add(privKeyPath);
	    command.add(numBackends);
	    return command;
	}

	private List<String> generateCommandExistingForWin(String wslScriptPath, String deploymentMethod,
			String isProvisioning, String username, String ipAddress, String productName, String certificateName,
			String nginxCertName, String productPrefix, String wslProductPath, String wslPrivKeyPath,
			String numBackends) {
		List<String> command = new ArrayList<>();
	    command.add("wsl");
	    command.add("bash");
	    command.add(wslScriptPath); 
	    command.add(deploymentMethod);
	    command.add(isProvisioning);
	    command.add(username);
	    command.add(ipAddress);
	    command.add(productName);
	    command.add(certificateName);
	    command.add(nginxCertName);
	    command.add(productPrefix);
	    command.add(wslProductPath);
	    command.add(wslPrivKeyPath);
	    command.add(numBackends);
	    return command;
	}

    
    
    public DeploymentTargetPage getDeploymentTargetPage() {
        return deploymentTargetPage;
    }
    
    public SelectDeploymentPage getDeploymentPage() {
        return deploymentPage;
    }
    
    
    public void zipAndCopyLib() {
    	WinVMJConsole.println("Memindahkan dan copy library vmj untuk digunakan");

    	try {
    	    Bundle bundle = Platform.getBundle("de.ovgu.featureide.core.winvmj");

    	    URL libURL = FileLocator.toFileURL(bundle.getEntry("resources/winvmj-libraries"));
    	    File libDir = new File(libURL.toURI());

    	    URL targetURL = FileLocator.toFileURL(bundle.getEntry("resources/deployment/systemd/products"));
    	    File targetDir = new File(targetURL.toURI());

    	    String zipFileName = "prices_product_libraries.zip";

    	    Path sourceFolder = libDir.toPath();
    	    Path targetFolder = targetDir.toPath();
    	    Path zipTarget = targetFolder.resolve(zipFileName);

    	    zipDirectory(sourceFolder, zipTarget);

    	    moveFileWithJava(zipTarget, targetFolder);

    	} catch (Exception e) {
    	    WinVMJConsole.println("[ERROR] Gagal meng-zip dan memindahkan: " + e.getMessage());
    	    e.printStackTrace();
    	}
    }
    public static void zipDirectory(Path sourceDir, Path zipFilePath) throws IOException{
        try (ZipOutputStream zipOut = new ZipOutputStream(Files.newOutputStream(zipFilePath))) {
            Files.walk(sourceDir)
                .filter(path -> !Files.isDirectory(path))
                .forEach(path -> {
                    ZipEntry zipEntry = new ZipEntry(sourceDir.relativize(path).toString().replace("\\", "/"));
                    try {
                        zipOut.putNextEntry(zipEntry);
                        Files.copy(path, zipOut);
                        zipOut.closeEntry();
                    } catch (IOException e) {
                        System.err.println("[ZIP ERROR] Gagal menambahkan file: " + path + " → " + e.getMessage());
                    }
                });
            WinVMJConsole.println("[ZIP] ZIP berhasil dibuat di: " + zipFilePath);
        } catch (IOException e) {
        	throw new IOException("Gagal membuat ZIP: " + e.getMessage(), e);
        }
    }

    public static void moveFileWithJava(Path source, Path targetDir) throws IOException {
        try {
            Files.createDirectories(targetDir);
            Path targetPath = targetDir.resolve(source.getFileName());
            Files.move(source, targetPath, StandardCopyOption.REPLACE_EXISTING);
            WinVMJConsole.println("[MOVE] File dipindahkan ke: " + targetPath);
        } catch (IOException e) {
            throw new IOException("Gagal memindahkan file dari " + source + " ke " + targetDir, e);
        }
    }



}
