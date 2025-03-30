package de.ovgu.featureide.core.winvmj.templates.impl;

import java.util.List;

import de.ovgu.featureide.core.IFeatureProject;

public class MicroserviceProductClassRenderer extends ProductClassRenderer {
		
	public MicroserviceProductClassRenderer(IFeatureProject project,
			List<String> routingFeatures) {
		super(project);
		this.selectedFeature = routingFeatures;
	}
}