package id.ac.ui.cs.prices.winvmj.composer.monitoring;

import id.ac.ui.cs.prices.winvmj.composer.Utils;
import id.ac.ui.cs.prices.winvmj.composer.monitoring.injector.DbMetricsInjector;
import id.ac.ui.cs.prices.winvmj.composer.monitoring.injector.HttpMetricsInjector;
import id.ac.ui.cs.prices.winvmj.composer.monitoring.injector.LoggingInjector;
import id.ac.ui.cs.prices.winvmj.composer.monitoring.injector.MethodMetricsInjector;
import id.ac.ui.cs.prices.winvmj.composer.monitoring.injector.RepositoryInjector;
import id.ac.ui.cs.prices.winvmj.composer.monitoring.injector.TracingInjector;
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
 * Each injector is independent — applies its own delta.
 *
 * Ordering:
 * 1. RepositoryInjector — generate bare proxy + inject constructor (if any DB-level concern)
 * 2. DbMetricsInjector — AST modify RepositoryImpl with metrics wrapping
 * 3. MethodMetricsInjector — AST modify ServiceImpl + ResourceImpl + RepositoryImpl with method timing
 * 4. LoggingInjector — AST modify ServiceImpl + ResourceImpl + RepositoryImpl with SLF4J logging
 * 5. HttpMetricsInjector — AST modify ResourceImpl @Route methods with HTTP metrics
 * 6. TracingInjector — AST modify RepositoryImpl + ServiceImpl + ResourceImpl with spans (outermost)
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

                boolean dbMetrics = MonitoringUtils.isDbMetricsEnabled(selectedFeatures, featureName);
                boolean methodMetrics = MonitoringUtils.isMethodMetricsEnabled(selectedFeatures, featureName);
                boolean logging = MonitoringUtils.isLoggingEnabled(selectedFeatures, featureName);
                boolean tracing = MonitoringUtils.isTracingEnabled(selectedFeatures, featureName);

                // 1. Generate bare RepositoryImpl proxy if any concern needs it
                if (dbMetrics || methodMetrics || logging || tracing) {
                    moduleDirs.forEach(dir -> RepositoryInjector.inject(dir, featureName));
                }
                // 2. Inject DB metrics into RepositoryImpl
                if (dbMetrics) {
                    moduleDirs.forEach(dir -> DbMetricsInjector.inject(dir, featureName));
                }
                // 3. Inject method metrics into ServiceImpl + ResourceImpl + RepositoryImpl
                if (methodMetrics) {
                    moduleDirs.forEach(dir -> MethodMetricsInjector.inject(dir, featureName));
                }
                // 4. Inject logging into ServiceImpl + ResourceImpl + RepositoryImpl
                if (logging) {
                    moduleDirs.forEach(dir -> LoggingInjector.inject(dir, featureName));
                }
                // 5. Inject HTTP metrics into ResourceImpl @Route methods
                if (MonitoringUtils.isHttpMetricsEnabled(selectedFeatures, featureName)) {
                    moduleDirs.forEach(dir -> HttpMetricsInjector.inject(dir, featureName));
                }
                // 6. Inject tracing spans into RepositoryImpl + ServiceImpl + ResourceImpl (outermost)
                if (tracing) {
                    moduleDirs.forEach(dir -> TracingInjector.inject(dir, featureName));
                }
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
