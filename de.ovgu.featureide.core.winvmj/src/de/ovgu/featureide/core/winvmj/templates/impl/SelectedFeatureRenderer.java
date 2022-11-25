package de.ovgu.featureide.core.winvmj.templates.impl;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;

import de.ovgu.featureide.core.winvmj.templates.ReactTemplateRenderer;

public class SelectedFeatureRenderer extends ReactTemplateRenderer {

	public SelectedFeatureRenderer(String[] selectedFeature) {
		super(selectedFeature, null);
	}

	public SelectedFeatureRenderer(String[] selectedFeature, Map<String, Object> featureMap) {
		super(selectedFeature, featureMap);
	}

	@Override
	protected Map<String, Object> extractDataModel() {
		// TODO Auto-generated method stub
		Map<String, Object> dataModel = new HashMap<>();
		dataModel.put("features", selectedFeature);
		return dataModel;
	}

	@Override
	protected String loadTemplateFilename() {
		// TODO Auto-generated method stub
		return "SelectedFeature";
	}

	@Override
	protected IFile getOutputFile(IProject targetProject) {
		// TODO Auto-generated method stub
		return targetProject.getFile("SelectedFeature");
	}

}
