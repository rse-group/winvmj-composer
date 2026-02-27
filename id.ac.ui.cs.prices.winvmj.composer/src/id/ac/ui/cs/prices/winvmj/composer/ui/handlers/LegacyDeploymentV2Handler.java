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
import id.ac.ui.cs.prices.winvmj.composer.ui.wizards.LegacyDeploymentWizard;
import id.ac.ui.cs.prices.winvmj.composer.ui.wizards.LegacyDeploymentWizardV2;
import id.ac.ui.cs.prices.winvmj.composer.ui.wizards.FeatureWizard;
import id.ac.ui.cs.prices.winvmj.composer.ui.wizards.models.NFRDefinition.*;
import id.ac.ui.cs.prices.winvmj.composer.ui.wizards.models.NFRDefinition.utils.NFRDefinitionUtil;

import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Shell;

public class LegacyDeploymentV2Handler extends AFeatureProjectHandler {
    private static Instance instanceType;
    private static String zipPath;
    private static Provider providerType;
    private static Region region;
    private static TPS tps;
    private static Transaction transaction;
    private static String productName = "";

    public static String getZipPath() {
        return zipPath;
    }

    public static String getProductName() {
        return productName;
    }

    public static Instance getInstance() {
        return instanceType;
    }

    public static Provider getProvider() {
        return providerType;
    }

    public static Region getRegion() {
        return region;
    }

    public static TPS getTPS() {
        return tps;
    }

    public static Transaction getTransaction() {
        return transaction;
    }

    public static void setInstanceType(Instance instance) {
        instanceType = instance;
    }

    public static void setProviderType(Provider provider) {
        providerType = provider;
    }

    public static void setRegion(Region reg) {
        region = reg;
    }

    public static void setTransaction(Transaction trans) {
        transaction = trans;
    }

    public static void setTPS(TPS t) {
        tps = t;
    }

    public static void setDefaultTPS() {
        WinVMJConsole.println("[INFO] Setting Default TPS to 100");
        tps = new TPS("100");
    }

    public static void setDefaultTransaction() {
        WinVMJConsole.println("[INFO] Setting Default Transaction to 1000");
        transaction = new Transaction("1000");
    }

    public static void setDefaultRegion() {
        WinVMJConsole.println("[INFO] Setting Default Region to Singapore");
        region = new Region("Singapore");
    }

	@Override
	protected void singleAction(IFeatureProject project) {
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
        processNFRSelectedFeatureIntoNFRModel(project);

        if (!validateProviderAndInstanceNotNull()) {
            WinVMJConsole.println("[ERROR] Please define the Provider and Instance!");
            return;
        }

        WinVMJConsole.println("\n=== Opening Deployment V2 Wizard ===");
		
        openDeploymentV2Wizard(project);
	}

    private boolean validateProviderAndInstanceNotNull() {
        return providerType != null && instanceType != null;
    }

    private void processNFRSelectedFeatureIntoNFRModel(IFeatureProject project) {
        Map<String, String> nfrMap;
        Path projectRoot = project.getProject().getLocation().toFile().toPath();
        Path jsonPath = projectRoot.resolve("nfr_to_model.json");
        
        try {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            String jsonContent = Files.readString(jsonPath);
            nfrMap = gson.fromJson(jsonContent, new TypeToken<Map<String, String>>(){}.getType());
        }

        catch (Exception e) {
            WinVMJConsole.println("An error occurred: " + e.getMessage());
            nfrMap = new HashMap<>();
        }

        Configuration currentConfig = project.loadCurrentConfiguration();

        Set<IFeature> selected = new HashSet<>(currentConfig.getSelectedFeatures());
        String nfrPrefix = NFRDefinitionUtil.getNFRPrefix(selected);
        selected = selected.stream().filter(feature -> feature.getName().startsWith(nfrPrefix)).collect(Collectors.toSet());

        for (IFeature feature : selected) {
            boolean hasSelectedChild = feature.getStructure().getChildren().stream()
                .map(c -> c.getFeature())
                .anyMatch(selected::contains);

            if (!hasSelectedChild) {
                IFeature parent = getTopSelectedParent(feature, selected);
                if (parent != null) {
                    mapConfigIntoNFRModel(NFRDefinitionUtil.formatDotPrefix(parent.getName()), NFRDefinitionUtil.formatDotPrefix(feature.getName()), nfrMap);
                }
            }
        }

        WinVMJConsole.println("NFR Model is mapped!");
    }

    private void mapConfigIntoNFRModel(String parent, String feature, Map<String, String> nfrMap) {
        if (!nfrMap.containsKey(parent)) {
            WinVMJConsole.println("[WARNING] The feature " + parent + " is not defined in your nfr_to_model.json");
            return;
        }

        switch (nfrMap.get(parent)) {
            case "Provider" -> providerType = new Provider(feature);
            case "Instance" -> instanceType = new Instance(feature);
            case "Transaction" -> transaction = new Transaction(feature);
            case "TPS" -> tps = new TPS(feature);
            case "Region" -> region = new Region(feature);
            default -> WinVMJConsole.println("[WARNING] The feature " + parent + " is not available in the current model");
        }
    }

    private void zipSrcGen(IFeatureProject project, String currentConfig) {
        try {
            Path srcGen = project.getProject().getLocation().toFile().toPath().resolve("src-gen/" + currentConfig);

            if (!Files.exists(srcGen)) {
                WinVMJConsole.println("[ERROR] src-gen folder does not exist!");
                return;
            }
            
            Path zipFile = project.getProject().getLocation().toFile().toPath().resolve(currentConfig + ".zip");

            try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFile.toFile()))) {
                Files.walkFileTree(srcGen, new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        Path relative = srcGen.relativize(file);
                        String entryName = currentConfig + "/" + relative.toString().replace("\\", "/");
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

    private IFeature getTopSelectedParent(IFeature feature, Set<IFeature> selected) {
        IFeatureStructure parentStruct = feature.getStructure().getParent();
        if (parentStruct == null) return null;

        IFeature parent = parentStruct.getFeature();
        if (!selected.contains(parent)) {
            return null;
        }

        if (parent.getName().contains("NFR")) {
            return null;
        }

        IFeature higher = getTopSelectedParent(parent, selected);
        return (higher != null) ? higher : parent;
    }

    private Shell getShell() {
        return org.eclipse.ui.PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
    }

    private void openDeploymentV2Wizard(IFeatureProject project) {
        Shell shell = getShell();
        LegacyDeploymentWizardV2 deploymentWizard = new LegacyDeploymentWizardV2(project);
        WizardDialog wizardDialog = new WizardDialog(shell, deploymentWizard);
        wizardDialog.open();
    }
}
