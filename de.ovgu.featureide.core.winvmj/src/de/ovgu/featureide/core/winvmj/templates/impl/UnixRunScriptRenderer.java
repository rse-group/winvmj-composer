package de.ovgu.featureide.core.winvmj.templates.impl;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IFile;

import de.ovgu.featureide.core.IFeatureProject;
import de.ovgu.featureide.core.winvmj.core.WinVMJProduct;
import de.ovgu.featureide.core.winvmj.templates.TemplateRenderer;

public class UnixRunScriptRenderer extends TemplateRenderer {

    private String dbUsername;
    private String dbPassword;

    public UnixRunScriptRenderer(IFeatureProject project,
            String dbUsername, String dbPassword) {
        super(project);
        this.dbUsername = dbUsername;
        this.dbPassword = dbPassword;
    }

    protected Map<String, Object> extractDataModel(WinVMJProduct product) {
        Map<String, Object> dataModel = new HashMap<>();

        dataModel.put("dbname", product.getProductQualifiedName().replace(".", "_"));
        dataModel.put("product", product.getProductQualifiedName());
        dataModel.put("dbUsername", dbUsername);
        dataModel.put("dbPassword", dbPassword);
        dataModel.put("SQLFolder", "sql");
        return dataModel;
    }

    protected String loadTemplateFilename() {
        return "run.sh";
    }

    protected IFile getOutputFile(WinVMJProduct product) {
        return project.getProject().getFolder("src-gen")
                .getFolder(product.getProductName())
                .getFile("run.sh");
    }
}
