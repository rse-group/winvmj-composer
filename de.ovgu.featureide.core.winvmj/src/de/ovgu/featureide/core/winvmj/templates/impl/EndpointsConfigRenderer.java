package de.ovgu.featureide.core.winvmj.templates.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IFile;

import de.ovgu.featureide.core.IFeatureProject;
import de.ovgu.featureide.core.winvmj.core.WinVMJProduct;
import de.ovgu.featureide.core.winvmj.templates.TemplateRenderer;

public class EndpointsConfigRenderer extends TemplateRenderer {
    public EndpointsConfigRenderer(IFeatureProject project) {
        super(project);
    }

    protected Map<String, Object> extractDataModel(WinVMJProduct product) {
        Map<String, Object> dataModel = new HashMap<>();

        dataModel.put("excludedEndpoints", extractExcludedEndpoints(product));

        return dataModel;
    }

    protected String loadTemplateFilename() {
        return "endpoints.json";
    }

    protected IFile getOutputFile(WinVMJProduct product) {
        return project.getProject().getFolder("src-gen")
                .getFolder(product.getProductName())
                .getFile("endpoints.json");
    }

    private List<String> extractExcludedEndpoints(WinVMJProduct product) {
        List<String> excludedEndpoints = new ArrayList<>();

        // TODO: Integrate with feature model and feature selection

        return excludedEndpoints;
    }
}
