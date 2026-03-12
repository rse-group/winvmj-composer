package id.ac.ui.cs.prices.winvmj.composer.monitoring;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Utility class for monitoring feature detection and configuration.
 * Handles parsing of Mon_ prefixed features from the feature model.
 */
public class MonitoringUtils {
	
	// Monitoring feature constants
	public static final String MON_PREFIX = "Mon_";
	public static final String FEATURE_MONITORING = "Monitoring";
	public static final String FEATURE_JVM_METRICS = "JvmMetrics";
	
	// Monitoring types (used as suffix after Mon_FeatureName_)
	public static final String MON_HTTP_METRICS = "HttpMetrics";
	public static final String MON_DB_METRICS = "DbMetrics";
	public static final String MON_METHOD_METRICS = "MethodMetrics";
	public static final String MON_TRACING = "Tracing";
	public static final String MON_LOGGING = "Logging";
	
	// Log level suffixes
	public static final String LOG_NONE = "LogNone";
	public static final String LOG_INFO = "LogInfo";
	public static final String LOG_VERBOSE = "LogVerbose";
	
	// Log level values
	public static final String LEVEL_NONE = "NONE";
	public static final String LEVEL_INFO = "INFO";
	public static final String LEVEL_VERBOSE = "VERBOSE";
	
	// Feature monitoring config keys
	public static final String CONFIG_FEATURE_NAME = "featureName";
	public static final String CONFIG_FEATURE_NAME_LOWER = "featureNameLower";
	public static final String CONFIG_ENABLE_HTTP_METRICS = "enableHttpMetrics";
	public static final String CONFIG_ENABLE_DB_METRICS = "enableDbMetrics";
	public static final String CONFIG_ENABLE_METHOD_METRICS = "enableMethodMetrics";
	public static final String CONFIG_ENABLE_TRACING = "enableTracing";
	public static final String CONFIG_LOGGING_LEVEL = "loggingLevel";
	public static final String CONFIG_MONITORING_TYPES = "monitoringTypes";
	
	// MonitoringInfos config keys
	public static final String INFO_JVM_METRICS_ENABLED = "jvmMetricsEnabled";
	public static final String INFO_FEATURE_CONFIGS = "featureConfigs";
	
	// Module naming
	public static final String MONITORING_MODULE_SUFFIX = ".monitoring.aspect";
	
	/**
	 * Check if Monitoring feature is selected in the current configuration.
	 */
	public static boolean isMonitoringEnabled(Set<String> selectedFeatures) {
		return selectedFeatures.contains(FEATURE_MONITORING);
	}
	
	/**
	 * Check if global JvmMetrics (Mon_JvmMetrics) is selected.
	 */
	public static boolean isJvmMetricsEnabled(Set<String> selectedFeatures) {
		return selectedFeatures.contains(MON_PREFIX + FEATURE_JVM_METRICS);
	}
	
	/**
	 * Get all features that have monitoring enabled.
	 * Parses Mon_FeatureName patterns and returns the feature names.
	 */
	public static Set<String> getMonitoredFeatures(Set<String> selectedFeatures) {
		Set<String> monitoredFeatures = new HashSet<>();
		for (String feature : selectedFeatures) {
			if (feature.startsWith(MON_PREFIX)) {
				String withoutPrefix = feature.substring(MON_PREFIX.length());
				// Mon_Overdraft, Mon_Interest, etc. (no second underscore = feature monitoring group)
				if (!withoutPrefix.contains("_") && !withoutPrefix.equals(FEATURE_JVM_METRICS)) {
					monitoredFeatures.add(withoutPrefix);
				}
			}
		}
		return monitoredFeatures;
	}
	
	/**
	 * Check if a specific feature has HttpMetrics monitoring enabled.
	 * @param featureName The functional feature name (e.g., "Overdraft")
	 */
	public static boolean isHttpMetricsEnabled(Set<String> selectedFeatures, String featureName) {
		return selectedFeatures.contains(MON_PREFIX + featureName + "_" + MON_HTTP_METRICS);
	}
	
	/**
	 * Check if a specific feature has DbMetrics monitoring enabled.
	 * @param featureName The functional feature name (e.g., "Overdraft")
	 */
	public static boolean isDbMetricsEnabled(Set<String> selectedFeatures, String featureName) {
		return selectedFeatures.contains(MON_PREFIX + featureName + "_" + MON_DB_METRICS);
	}
	
	/**
	 * Check if a specific feature has MethodMetrics monitoring enabled.
	 * @param featureName The functional feature name (e.g., "Overdraft")
	 */
	public static boolean isMethodMetricsEnabled(Set<String> selectedFeatures, String featureName) {
		return selectedFeatures.contains(MON_PREFIX + featureName + "_" + MON_METHOD_METRICS);
	}
	
	/**
	 * Check if a specific feature has Tracing monitoring enabled.
	 * @param featureName The functional feature name (e.g., "Overdraft")
	 */
	public static boolean isTracingEnabled(Set<String> selectedFeatures, String featureName) {
		return selectedFeatures.contains(MON_PREFIX + featureName + "_" + MON_TRACING);
	}
	
	/**
	 * Get the logging level for a specific feature.
	 * Returns "NONE", "INFO", or "VERBOSE" based on which Mon_Feature_Log* is selected.
	 * @param featureName The functional feature name (e.g., "Overdraft")
	 */
	public static String getLoggingLevel(Set<String> selectedFeatures, String featureName) {
		if (selectedFeatures.contains(MON_PREFIX + featureName + "_" + LOG_VERBOSE)) {
			return LEVEL_VERBOSE;
		} else if (selectedFeatures.contains(MON_PREFIX + featureName + "_" + LOG_INFO)) {
			return LEVEL_INFO;
		} else if (selectedFeatures.contains(MON_PREFIX + featureName + "_" + LOG_NONE)) {
			return LEVEL_NONE;
		}
		return LEVEL_NONE;
	}
	
	/**
	 * Parse a monitoring feature name and extract its components.
	 * @param monFeature Feature name starting with Mon_ (e.g., "Mon_Overdraft_HttpMetrics")
	 * @return String array: [featureName, monitoringType] or null if invalid
	 */
	public static String[] parseMonitoringFeature(String monFeature) {
		if (!monFeature.startsWith(MON_PREFIX)) {
			return null;
		}
		String withoutPrefix = monFeature.substring(MON_PREFIX.length());
		int underscoreIdx = withoutPrefix.indexOf('_');
		if (underscoreIdx == -1) {
			// Mon_JvmMetrics or Mon_Overdraft (no monitoring type)
			return new String[] { withoutPrefix, null };
		}
		return new String[] {
			withoutPrefix.substring(0, underscoreIdx),
			withoutPrefix.substring(underscoreIdx + 1)
		};
	}
	
	/**
	 * Get all monitoring types enabled for a specific feature.
	 * @param featureName The functional feature name (e.g., "Overdraft")
	 * @return Set of monitoring types (e.g., {"HttpMetrics", "DbMetrics", "Tracing"})
	 */
	public static Set<String> getMonitoringTypesForFeature(Set<String> selectedFeatures, String featureName) {
		Set<String> monitoringTypes = new HashSet<>();
		String prefix = MON_PREFIX + featureName + "_";
		
		for (String feature : selectedFeatures) {
			if (feature.startsWith(prefix)) {
				String monType = feature.substring(prefix.length());
				// Skip log level specifics, just add the type
				if (monType.equals(MON_HTTP_METRICS) || 
					monType.equals(MON_DB_METRICS) || 
					monType.equals(MON_METHOD_METRICS) || 
					monType.equals(MON_TRACING)) {
					monitoringTypes.add(monType);
				} else if (monType.startsWith("Log")) {
					// Add Logging as type (not LogNone/LogInfo/LogVerbose)
					monitoringTypes.add(MON_LOGGING);
				}
			}
		}
		return monitoringTypes;
	}
	
	/**
	 * Build a complete monitoring config map for a feature.
	 * Returns all monitoring settings in one call.
	 * @param featureName The functional feature name (e.g., "Overdraft")
	 */
	public static Map<String, Object> getFeatureMonitoringConfig(Set<String> selectedFeatures, String featureName) {
		Map<String, Object> config = new HashMap<>();
		config.put(CONFIG_FEATURE_NAME, featureName);
		config.put(CONFIG_FEATURE_NAME_LOWER, featureName.toLowerCase());
		config.put(CONFIG_ENABLE_HTTP_METRICS, isHttpMetricsEnabled(selectedFeatures, featureName));
		config.put(CONFIG_ENABLE_DB_METRICS, isDbMetricsEnabled(selectedFeatures, featureName));
		config.put(CONFIG_ENABLE_METHOD_METRICS, isMethodMetricsEnabled(selectedFeatures, featureName));
		config.put(CONFIG_ENABLE_TRACING, isTracingEnabled(selectedFeatures, featureName));
		config.put(CONFIG_LOGGING_LEVEL, getLoggingLevel(selectedFeatures, featureName));
		config.put(CONFIG_MONITORING_TYPES, getMonitoringTypesForFeature(selectedFeatures, featureName));
		return config;
	}
	
	/**
	 * Get the monitoring aspect module name for a product.
	 * Format: {spl}.monitoring.aspect (e.g., bankaccount.monitoring.aspect)
	 * Note: Does NOT use .product. to avoid being treated as a product module.
	 */
	public static String getMonitoringModuleName(String productQualifiedName) {
		String[] parts = productQualifiedName.split("\\.");
		if (parts.length >= 1) {
			return parts[0] + MONITORING_MODULE_SUFFIX;
		}
		return MONITORING_MODULE_SUFFIX.substring(1); // Remove leading dot
	}
	
	
	
	/**
	 * Get complete monitoring information for all monitored features.
	 * Returns a structured map containing:
	 * - jvmMetricsEnabled: boolean
	 * - featureConfigs: List of feature monitoring configs (each with its own loggingLevel)
	 * 
	 * @param selectedFeatures The set of selected features from UVL config
	 * @return Map with monitoring info structure
	 */
	public static Map<String, Object> getMonitoringInfos(Set<String> selectedFeatures) {
		Map<String, Object> infos = new HashMap<>();
		
		// Global settings
		infos.put(INFO_JVM_METRICS_ENABLED, isJvmMetricsEnabled(selectedFeatures));
		
		// Per-feature configs
		Set<String> monitoredFeatures = getMonitoredFeatures(selectedFeatures);
		List<Map<String, Object>> featureConfigs = new ArrayList<>();
		for (String featureName : monitoredFeatures) {
			featureConfigs.add(getFeatureMonitoringConfig(selectedFeatures, featureName));
		}
		infos.put(INFO_FEATURE_CONFIGS, featureConfigs);
		
		return infos;
	}
}
