package id.ac.ui.cs.prices.winvmj.composer.templates.impl;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IFile;

import de.ovgu.featureide.core.IFeatureProject;
import id.ac.ui.cs.prices.winvmj.composer.core.WinVMJProduct;
import id.ac.ui.cs.prices.winvmj.composer.templates.TemplateRenderer;

public class WindowsRunScriptRenderer extends TemplateRenderer {

    private String dbUsername;
    private String dbPassword;

    public WindowsRunScriptRenderer(IFeatureProject project,
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
        return "run.bat";
    }

    protected IFile getOutputFile(WinVMJProduct product) {
        return project.getProject().getFolder("src-gen")
                .getFolder(product.getProductName())
                .getFile("run.bat");
    }
}
