package id.ac.ui.cs.prices.winvmj.composer.monitoring;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.eclipse.core.resources.IFile;

import de.ovgu.featureide.core.IFeatureProject;

/**
 * Utility class for monitoring configuration.
 * Reads monitoring settings from monitoring/{configName}.properties.
 */
public class MonitoringUtils {

	// Programming mode: switch between AOP (AspectJ) and DOP (source modification)
	public enum MonitoringMode { AOP, DOP }
	public static final MonitoringMode MONITORING_MODE = MonitoringMode.DOP;

	// Feature monitoring config keys (used by downstream renderers/templates)
	public static final String CONFIG_FEATURE_NAME = "featureName";
	public static final String CONFIG_FEATURE_NAME_LOWER = "featureNameLower";
	public static final String CONFIG_ENABLE_HTTP_METRICS = "enableHttpMetrics";
	public static final String CONFIG_ENABLE_DB_METRICS = "enableDbMetrics";
	public static final String CONFIG_ENABLE_METHOD_METRICS = "enableMethodMetrics";
	public static final String CONFIG_ENABLE_TRACING = "enableTracing";
	public static final String CONFIG_ENABLE_LOGGING = "enableLogging";
	public static final String CONFIG_MONITORING_TYPES = "monitoringTypes";

	// MonitoringInfos config keys
	public static final String INFO_JVM_METRICS_ENABLED = "jvmMetricsEnabled";
	public static final String INFO_FEATURE_CONFIGS = "featureConfigs";

	// Module naming
	public static final String MONITORING_MODULE_SUFFIX = ".monitoring.aspect";

	// Monitoring type keys (matching properties file format)
	private static final String[] MONITORING_TYPE_KEYS = {
		"httpMetrics", "dbMetrics", "methodMetrics", "tracing", "logging"
	};
	// Display names for monitoring types (for template compatibility)
	private static final String[] MONITORING_TYPE_NAMES = {
		"HttpMetrics", "DbMetrics", "MethodMetrics", "Tracing", "Logging"
	};

	/**
	 * Load monitoring properties from monitoring/{configName}.properties.
	 * Returns null if file doesn't exist.
	 */
	private static Properties loadMonitoringProps(IFeatureProject project) {
		if (project == null) return null;
		try {
			Path configPath = project.getCurrentConfiguration();
			String configName = configPath.toFile().getName().replace(".xml", "");
			IFile propsFile = project.getProject().getFile("monitoring/" + configName + ".properties");
			if (!propsFile.exists()) return null;

			Properties props = new Properties();
			try (InputStream is = propsFile.getContents()) {
				props.load(is);
			}
			return props;
		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * Check if monitoring is enabled.
	 */
	public static boolean isMonitoringEnabled(IFeatureProject project) {
		Properties props = loadMonitoringProps(project);
		if (props == null) return false;
		if (Boolean.parseBoolean(props.getProperty("jvmMetrics", "false"))) return true;

		for (String key : props.stringPropertyNames()) {
			if (key.equals("jvmMetrics")) continue;
			if (Boolean.parseBoolean(props.getProperty(key, "false"))) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Check if global JVM metrics is enabled.
	 */
	public static boolean isJvmMetricsEnabled(IFeatureProject project) {
		Properties props = loadMonitoringProps(project);
		return props != null && Boolean.parseBoolean(props.getProperty("jvmMetrics", "false"));
	}

	/**
	 * Get all features that have at least one monitoring type enabled.
	 */
	public static Set<String> getMonitoredFeatures(IFeatureProject project) {
		Properties props = loadMonitoringProps(project);
		if (props == null) return Collections.emptySet();
		Set<String> features = new HashSet<>();
		for (String key : props.stringPropertyNames()) {
			if (key.equals("jvmMetrics")) continue;
			int dot = key.indexOf('.');
			if (dot > 0 && Boolean.parseBoolean(props.getProperty(key, "false"))) {
				features.add(key.substring(0, dot));
			}
		}
		return features;
	}

	public static boolean isHttpMetricsEnabled(IFeatureProject project, String featureName) {
		Properties props = loadMonitoringProps(project);
		return props != null && Boolean.parseBoolean(props.getProperty(featureName + ".httpMetrics", "false"));
	}

	public static boolean isDbMetricsEnabled(IFeatureProject project, String featureName) {
		Properties props = loadMonitoringProps(project);
		return props != null && Boolean.parseBoolean(props.getProperty(featureName + ".dbMetrics", "false"));
	}

	public static boolean isMethodMetricsEnabled(IFeatureProject project, String featureName) {
		Properties props = loadMonitoringProps(project);
		return props != null && Boolean.parseBoolean(props.getProperty(featureName + ".methodMetrics", "false"));
	}

	public static boolean isTracingEnabled(IFeatureProject project, String featureName) {
		Properties props = loadMonitoringProps(project);
		return props != null && Boolean.parseBoolean(props.getProperty(featureName + ".tracing", "false"));
	}

	public static boolean isLoggingEnabled(IFeatureProject project, String featureName) {
		Properties props = loadMonitoringProps(project);
		return props != null && Boolean.parseBoolean(props.getProperty(featureName + ".logging", "false"));
	}

	/**
	 * Get all monitoring types enabled for a specific feature.
	 */
	public static Set<String> getMonitoringTypesForFeature(IFeatureProject project, String featureName) {
		Properties props = loadMonitoringProps(project);
		if (props == null) return Collections.emptySet();
		Set<String> types = new HashSet<>();
		for (int i = 0; i < MONITORING_TYPE_KEYS.length; i++) {
			if (Boolean.parseBoolean(props.getProperty(featureName + "." + MONITORING_TYPE_KEYS[i], "false"))) {
				types.add(MONITORING_TYPE_NAMES[i]);
			}
		}
		return types;
	}

	/**
	 * Build a complete monitoring config map for a feature.
	 */
	public static Map<String, Object> getFeatureMonitoringConfig(IFeatureProject project, String featureName) {
		Map<String, Object> config = new HashMap<>();
		config.put(CONFIG_FEATURE_NAME, featureName);
		config.put(CONFIG_FEATURE_NAME_LOWER, featureName.toLowerCase());
		config.put(CONFIG_ENABLE_HTTP_METRICS, isHttpMetricsEnabled(project, featureName));
		config.put(CONFIG_ENABLE_DB_METRICS, isDbMetricsEnabled(project, featureName));
		config.put(CONFIG_ENABLE_METHOD_METRICS, isMethodMetricsEnabled(project, featureName));
		config.put(CONFIG_ENABLE_TRACING, isTracingEnabled(project, featureName));
		config.put(CONFIG_ENABLE_LOGGING, isLoggingEnabled(project, featureName));
		config.put(CONFIG_MONITORING_TYPES, getMonitoringTypesForFeature(project, featureName));
		return config;
	}

	/**
	 * Get the monitoring aspect module name for a product.
	 */
	public static String getMonitoringModuleName(String productQualifiedName) {
		String[] parts = productQualifiedName.split("\\.");
		if (parts.length >= 1) {
			return parts[0] + MONITORING_MODULE_SUFFIX;
		}
		return MONITORING_MODULE_SUFFIX.substring(1);
	}

	/**
	 * Get complete monitoring information for all monitored features.
	 */
	public static Map<String, Object> getMonitoringInfos(IFeatureProject project) {
		Map<String, Object> infos = new HashMap<>();
		infos.put(INFO_JVM_METRICS_ENABLED, isJvmMetricsEnabled(project));

		Set<String> monitoredFeatures = getMonitoredFeatures(project);
		List<Map<String, Object>> featureConfigs = new ArrayList<>();
		for (String featureName : monitoredFeatures) {
			featureConfigs.add(getFeatureMonitoringConfig(project, featureName));
		}
		infos.put(INFO_FEATURE_CONFIGS, featureConfigs);

		return infos;
	}
}
