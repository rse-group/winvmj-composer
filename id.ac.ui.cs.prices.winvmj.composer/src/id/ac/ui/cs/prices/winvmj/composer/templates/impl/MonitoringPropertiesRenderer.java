package id.ac.ui.cs.prices.winvmj.composer.templates.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.core.resources.IFile;

import de.ovgu.featureide.core.IFeatureProject;
import id.ac.ui.cs.prices.winvmj.composer.core.WinVMJProduct;
import id.ac.ui.cs.prices.winvmj.composer.monitoring.MonitoringUtils;
import id.ac.ui.cs.prices.winvmj.composer.templates.TemplateRenderer;

public class MonitoringPropertiesRenderer extends TemplateRenderer {

	private Set<String> selectedFeatures;

	public MonitoringPropertiesRenderer(IFeatureProject project) {
		super(project);
		Set<String> features = project.loadCurrentConfiguration().getSelectedFeatureNames();
		selectedFeatures = features.stream()
			.map(name -> name.contains(".") ? name.substring(name.indexOf('.') + 1) : name)
			.collect(Collectors.toSet());
	}

	@Override
	protected Map<String, Object> extractDataModel(WinVMJProduct product) {
		Map<String, Object> dataModel = new HashMap<>();
		dataModel.put("monitoringEnabled", MonitoringUtils.isMonitoringEnabled(selectedFeatures));
		dataModel.put("monitoredFeatures", MonitoringUtils.getMonitoredFeatures(selectedFeatures));
		return dataModel;
	}

	@Override
	protected String loadTemplateFilename() {
		return "monitoring.properties";
	}

	@Override
	protected IFile getOutputFile(WinVMJProduct product) {
		return project.getProject().getFolder("src-gen")
				.getFolder(product.getProductName())
				.getFile("monitoring.properties");
	}
}
