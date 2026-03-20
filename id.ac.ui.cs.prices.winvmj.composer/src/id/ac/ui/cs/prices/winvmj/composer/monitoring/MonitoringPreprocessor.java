package id.ac.ui.cs.prices.winvmj.composer.monitoring;

import id.ac.ui.cs.prices.winvmj.composer.Utils;
import id.ac.ui.cs.prices.winvmj.composer.monitoring.injector.HttpMetricsInjector;
import id.ac.ui.cs.prices.winvmj.composer.monitoring.injector.MethodMetricsInjector;
import id.ac.ui.cs.prices.winvmj.composer.runtime.WinVMJConsole;

import de.ovgu.featureide.core.IFeatureProject;

import org.eclipse.core.resources.IFolder;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * DOP Monitoring Preprocessor — orchestrator.
 * Reads feature config, dispatches to specific injectors.
 * Called from WinVMJComposer.composeProduct() as a single line.
 */
public class MonitoringPreprocessor {

    public static void process(IFeatureProject featureProject) {
        try {
            Set<String> selectedFeatures = featureProject.loadCurrentConfiguration()
                .getSelectedFeatureNames().stream()
                .map(name -> name.contains(".") ? name.substring(name.indexOf('.') + 1) : name)
                .collect(Collectors.toSet());

            if (!MonitoringUtils.isMonitoringEnabled(selectedFeatures)) return;

            Map<String, List<String>> featureToModuleMap = Utils.getFeatureToModuleMap(
                featureProject.getProject());
            IFolder buildFolder = featureProject.getBuildFolder();

            Set<String> monitoredFeatures = MonitoringUtils.getMonitoredFeatures(selectedFeatures);
            for (String featureName : monitoredFeatures) {
                Set<IFolder> moduleDirs = resolveModuleDirs(featureToModuleMap, buildFolder, featureName);
                if (moduleDirs.isEmpty()) continue;

                // Method metrics first (inner layer), then HTTP metrics (outer layer)
                if (MonitoringUtils.isMethodMetricsEnabled(selectedFeatures, featureName)) {
                    moduleDirs.forEach(dir -> MethodMetricsInjector.inject(dir, featureName));
                }
                if (MonitoringUtils.isHttpMetricsEnabled(selectedFeatures, featureName)) {
                    moduleDirs.forEach(dir -> HttpMetricsInjector.inject(dir, featureName));
                }
                // TODO: MethodMetricsInjector, DbMetricsInjector, TracingInjector, LoggingInjector
            }
        } catch (Exception e) {
            WinVMJConsole.println("[MonitoringPreprocessor] Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static Set<IFolder> resolveModuleDirs(
            Map<String, List<String>> featureToModuleMap, IFolder buildFolder, String featureName) {
        List<String> moduleNames = featureToModuleMap.get(featureName);
        if (moduleNames == null) return Set.of();
        return moduleNames.stream()
            .map(buildFolder::getFolder)
            .filter(IFolder::exists)
            .collect(Collectors.toSet());
    }
}
