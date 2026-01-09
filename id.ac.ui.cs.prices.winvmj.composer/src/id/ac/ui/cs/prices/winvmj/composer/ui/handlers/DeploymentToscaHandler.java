package id.ac.ui.cs.prices.winvmj.composer.ui.handlers;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import de.ovgu.featureide.fm.core.configuration.Configuration;


import de.ovgu.featureide.core.IFeatureProject;
import de.ovgu.featureide.fm.core.base.IFeatureStructure;
import de.ovgu.featureide.fm.core.base.IFeature;
import de.ovgu.featureide.fm.core.job.LongRunningMethod;
import de.ovgu.featureide.fm.core.job.LongRunningWrapper;
import de.ovgu.featureide.fm.core.job.monitor.IMonitor;
import de.ovgu.featureide.ui.handlers.base.AFeatureProjectHandler;
import id.ac.ui.cs.prices.winvmj.composer.compile.SourceCompiler;
import id.ac.ui.cs.prices.winvmj.composer.runtime.WinVMJConsole;
import id.ac.ui.cs.prices.winvmj.composer.ui.wizards.DeploymentToscaWizard;
import id.ac.ui.cs.prices.winvmj.composer.ui.wizards.DeploymentWizard;
import id.ac.ui.cs.prices.winvmj.composer.ui.wizards.FeatureWizard;
import id.ac.ui.cs.prices.winvmj.composer.ui.wizards.models.NFRDefinition.*;
import id.ac.ui.cs.prices.winvmj.composer.ui.wizards.models.NFRDefinition.utils.NFRDefinitionUtil;

import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Shell;

public class DeploymentToscaHandler extends AFeatureProjectHandler {
    private static String zipPath;
    private static String productName = "";

    public static String getZipPath() {
        return zipPath;
    }

    public static String getProductName() {
        return productName;
    }

	@Override
	protected void singleAction(IFeatureProject project) {
        /**
         * Deployment via TOSCA Wizard Handler Logic:
         * 
         * Get current product configuration -> Compile the src folder
         * -> Zip the src-gen -> Map all NFR data from configuration file into NFR Java Object -> Open wizard
         */
        
        String currentConfigFile = project.getCurrentConfiguration().toFile().getName();
        WinVMJConsole.println("Current Product Configuration: " + currentConfigFile);
        WinVMJConsole.println("\n=== Compiling src ===");
        long start = System.currentTimeMillis();
        SourceCompiler.compileSource(project);
        long finish = System.currentTimeMillis();
        double elapsedTime = (finish-start)/1000.0;
        WinVMJConsole.println("Compile process completed in " 
        + String.valueOf(elapsedTime) 
        + " seconds");

        WinVMJConsole.println("\n=== Zipping src-gen ===");
        productName = NFRDefinitionUtil.formatDotSuffix(currentConfigFile).toLowerCase();
        zipSrcGen(project, NFRDefinitionUtil.formatDotSuffix(currentConfigFile));
        
        WinVMJConsole.println("\n=== Transforming nfr_to_model.json ===");
        NFRDefinition.processNFRSelectedFeatureIntoNFRModel(project);

        WinVMJConsole.println("\n=== Transforming instance_type.json and region.json ===");
        NFRDefinition.processInstanceType(project);
        NFRDefinition.processRegion(project);

        if (!NFRDefinition.validateProviderAndInstanceNotNull()) {
            WinVMJConsole.println("[ERROR] Please define the Provider and Instance!");
            return;
        }

        WinVMJConsole.println("\n=== Opening Deployment V2 Wizard ===");
		
        openDeploymentV2Wizard(project);
	}


    private void zipSrcGen(IFeatureProject project, String currentConfig) {
        try {
            Path srcGen = project.getProject().getLocation().toFile().toPath().resolve("src-gen/" + currentConfig);

            if (!Files.exists(srcGen)) {
                WinVMJConsole.println("[ERROR] src-gen folder does not exist!");
                return;
            }
            
            Path zipFile = project.getProject().getLocation().toFile().toPath().resolve("application.zip");

            try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFile.toFile()))) {
                Files.walkFileTree(srcGen, new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        Path relative = srcGen.relativize(file);
                        String entryName = relative.toString().replace("\\", "/");
                        zos.putNextEntry(new ZipEntry(entryName));
                        Files.copy(file, zos);
                        zos.closeEntry();
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                        Path relative = srcGen.relativize(dir);
                        if (!relative.toString().isEmpty()) {
                            String entryName = currentConfig + "/" + relative.toString().replace("\\", "/") + "/";
                            zos.putNextEntry(new ZipEntry(entryName));
                            zos.closeEntry();
                        }
                        return FileVisitResult.CONTINUE;
                    }
                });
            }

            zipPath = zipFile.toString();

            WinVMJConsole.println("src-gen folder zipped at: " + zipFile.toString());
        }

        catch (IOException e) {
            WinVMJConsole.println("[ERROR] " + e.getMessage());
        }
    }

    private Shell getShell() {
        return org.eclipse.ui.PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
    }

    private void openDeploymentV2Wizard(IFeatureProject project) {
        Shell shell = getShell();
        DeploymentToscaWizard deploymentWizard = new DeploymentToscaWizard(project);
        WizardDialog wizardDialog = new WizardDialog(shell, deploymentWizard);
        wizardDialog.open();
    }
}
