package id.ac.ui.cs.prices.winvmj.composer.ui.wizards.models.NFRDefinition;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;

import de.ovgu.featureide.core.IFeatureProject;
import de.ovgu.featureide.fm.core.base.IFeatureStructure;
import de.ovgu.featureide.fm.core.base.IFeature;
import de.ovgu.featureide.fm.core.configuration.Configuration;
import id.ac.ui.cs.prices.winvmj.composer.runtime.WinVMJConsole;
import id.ac.ui.cs.prices.winvmj.composer.ui.wizards.models.NFRDefinition.utils.NFRDefinitionUtil;

public class NFRDefinition {
    private static Provider providerType;
    private static Region region;
    private static TPS tps;
    private static Transaction transaction;
    private static Instance instanceType;
    private static List<String> instances;
    private static List<String> regions;

    private static final Set<String> REGION_GROUPS = Set.of(
        "North America",
        "South America",
        "Europe",
        "Asia Pacific",
        "Middle East"
    );

    private NFRDefinition() {}

    public static void processNFRSelectedFeatureIntoNFRModel(IFeatureProject project) {
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

    public static void processRegion(IFeatureProject project) {
        Map<String, Map<String, List<String>>> regionMap;

        Path projectRoot = project.getProject().getLocation().toFile().toPath();
        Path jsonPath = projectRoot.resolve("region.json");

        try {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            String jsonContent = Files.readString(jsonPath);

            regionMap = gson.fromJson(
                jsonContent,
                new TypeToken<Map<String, Map<String, List<String>>>>() {}.getType()
            );

        } catch (Exception e) {
            WinVMJConsole.println("An error occurred: " + e.getMessage());
            return;
        }

        Configuration currentConfig = project.loadCurrentConfiguration();
        Set<IFeature> selected = new HashSet<>(currentConfig.getSelectedFeatures());
        String nfrPrefix = NFRDefinitionUtil.getNFRPrefix(selected);
        selected = selected.stream()
            .filter(feature -> feature.getName().startsWith(nfrPrefix))
            .collect(Collectors.toSet());

        String provider = null;
        String region = null;

        for (IFeature feature : selected) {
            String name = feature.getName().substring(nfrPrefix.length());

            if (name.equalsIgnoreCase("AWS") || name.equalsIgnoreCase("GCP")) {
                provider = name;
            }

            if (REGION_GROUPS.contains(name)) {
                region = name;
            }
        }

        if (provider == null || region == null) {
            WinVMJConsole.println("Provider or region not selected.");
            return;
        }

        regions = regionMap
            .getOrDefault(provider, Collections.emptyMap())
            .getOrDefault(region, Collections.emptyList());

        WinVMJConsole.println("Region resolved!");
    }

    public static void processInstanceType(IFeatureProject project) {
        Map<String, Map<String, List<String>>> instanceTypeMap;

        Path projectRoot = project.getProject().getLocation().toFile().toPath();
        Path jsonPath = projectRoot.resolve("instance_type.json");

        try {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            String jsonContent = Files.readString(jsonPath);

            instanceTypeMap = gson.fromJson(
                jsonContent,
                new TypeToken<Map<String, Map<String, List<String>>>>() {}.getType()
            );

        } catch (Exception e) {
            WinVMJConsole.println("An error occurred: " + e.getMessage());
            return;
        }

        Configuration currentConfig = project.loadCurrentConfiguration();
        Set<IFeature> selected = new HashSet<>(currentConfig.getSelectedFeatures());
        String nfrPrefix = NFRDefinitionUtil.getNFRPrefix(selected);
        selected = selected.stream()
            .filter(feature -> feature.getName().startsWith(nfrPrefix))
            .collect(Collectors.toSet());

        String provider = null;
        String category = null;

        for (IFeature feature : selected) {
            String name = feature.getName().substring(nfrPrefix.length());

            if (name.equalsIgnoreCase("AWS") || name.equalsIgnoreCase("GCP")) {
                provider = name;
            }

            if (name.equalsIgnoreCase("Low Cost") ||
                name.equalsIgnoreCase("High Performance") ||
                name.equalsIgnoreCase("Balance Price and Performance")) {
                category = name;
            }
        }

        if (provider == null || category == null) {
            WinVMJConsole.println("Provider or instance category not selected.");
            return;
        }

        instances = instanceTypeMap
            .getOrDefault(provider, Collections.emptyMap())
            .getOrDefault(category, Collections.emptyList());

        WinVMJConsole.println("Instance types resolved!");
    }


    private static void mapConfigIntoNFRModel(String parent, String feature, Map<String, String> nfrMap) {
        if (!nfrMap.containsKey(parent)) {
            WinVMJConsole.println("[WARNING] The feature " + parent + " is not defined in your nfr_to_model.json");
            return;
        }

        switch (nfrMap.get(parent)) {
            case "Provider" -> providerType = new Provider(feature);
            case "Instance" -> {
                if (feature.contains("-")) {
                    boolean replace = MessageDialog.openQuestion(
                        Display.getDefault().getActiveShell(),
                        "Update Instance Name",
                        "The instance name \"" + feature + "\" contains a hypen ('-').\n" +
                        "Do you want to replace '-' with '.'?"
                    );

                    if (replace) {
                        feature = feature.replace("-", ".");
                        WinVMJConsole.println("[INFO] Instance name updated to: " + feature);
                    }
                }
                instanceType = new Instance(feature);
            }
            case "Transaction" -> transaction = new Transaction(feature);
            case "TPS" -> tps = new TPS(feature);
            case "Region" -> region = new Region(feature);
            default -> WinVMJConsole.println("[WARNING] The feature " + parent + " is not available in the current model");
        }
    }

    private static IFeature getTopSelectedParent(IFeature feature, Set<IFeature> selected) {
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

    public static boolean validateProviderAndInstanceNotNull() {
        return providerType != null && instanceType != null;
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

    public static String[] getInstances() {
        return instances.toArray(new String[0]);
    }

    public static String[] getRegions() {
        return regions.toArray(new String[0]);
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
    
}
