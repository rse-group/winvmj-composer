package de.ovgu.featureide.core.winvmj.templates.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.io.InputStreamReader;
import java.io.Reader;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;

import de.ovgu.featureide.core.IFeatureProject;
import de.ovgu.featureide.core.winvmj.core.WinVMJProduct;
import de.ovgu.featureide.core.winvmj.runtime.WinVMJConsole;
import de.ovgu.featureide.core.winvmj.templates.MultiLevelDeltaTemplateRenderer;

public class MultiLevelDeltaResourceRenderer extends MultiLevelDeltaTemplateRenderer {
    private String featureVariationName;
    private List<String> defaultLibraries = new ArrayList<>(Arrays.asList(
        "import vmj.routing.route.Route;",
        "import vmj.routing.route.VMJExchange;",
        "import vmj.routing.route.exceptions.*;",
        String.format("import %s.%s;", coreModule, featureName),
        String.format("import %s.%sResource;", coreModule, featureName),
        String.format("import %s.%sResourceDecorator;", coreModule, featureName),
        String.format("import %s.%sResourceComponent;", coreModule, featureName),
        String.format("import %s.%sServiceComponent;", coreModule, featureName)
    ));

    public MultiLevelDeltaResourceRenderer(
        IFeatureProject project,
        String splName,
        String featureName,
        String featureFullyQualifiedName,
        String featureVariationName,
        String coreModule
    ) {
        super(
            project,
            splName,
            featureName,
            featureFullyQualifiedName,
            coreModule,
            "resource"
        );
        this.featureVariationName = featureVariationName;
    }

    @Override
    protected Map<String, Object> extractDataModel(WinVMJProduct product) {
        Map<String, Object> dataModel = new HashMap<>();

        dataModel.put("package", featureFullyQualifiedName);
        getRequiredBindings(dataModel);

        return dataModel;
    }

    private void getRequiredBindings(Map<String, Object> dataModel) {
        String concreteComponentClassFqn = "src/" + coreModule + "/";
        for (String coreModulePath: coreModule.split("\\.")) {
			concreteComponentClassFqn += coreModulePath + "/";
		}
		concreteComponentClassFqn += "resource/" + featureName + "ResourceImpl.java";

        StringBuilder content = new StringBuilder();
        IFile concreteComponentClass = project.getProject().getFile(concreteComponentClassFqn);
        try (Reader mapReader = new InputStreamReader(concreteComponentClass.getContents())) {
            char[] buffer = new char[1024];
			int numRead;

			while ((numRead = mapReader.read(buffer)) != -1) {
				content.append(buffer, 0, numRead);
			}
        } catch (Exception e) {
			WinVMJConsole.println(e.getMessage());
			for (StackTraceElement em : e.getStackTrace())
				WinVMJConsole.println(em.toString());
			e.printStackTrace();
		}

        String stringifiedContent = content.toString();
        List<String> requiredLibraries = extractRequiredLibraries(stringifiedContent);
        requiredLibraries.addAll(defaultLibraries);
        dataModel.put("requiredLibraries", requiredLibraries);

        String classContent = extractClassContent(stringifiedContent);
        if (!classContent.startsWith("public") && classContent.startsWith(" ")) {
            classContent = "public" + classContent;
        }
        
        classContent = classContent.replace(
            String.format("%sResourceComponent", featureName),
            String.format("%sResourceDecorator", featureName)
        );
        classContent = classContent.replace(
            String.format("call/%s", featureName.toLowerCase()),
            String.format("call/%s", featureVariationName.toLowerCase())
        );

        String constructor = String.format(
            "    public %sResourceImpl(%sResourceComponent recordController, %sServiceComponent recordService) {\n" +
            "        super(recordController);\n" +
            "        this.%sServiceImpl = new %sServiceImpl(recordService);\n" +
            "    }",
            featureName,
            featureName,
            featureName,
            featureName.toLowerCase(),
            featureName
        );
        String serviceField = String.format(
            "%sServiceImpl %sServiceImpl",
            featureName,
            featureName.toLowerCase()
        );
        classContent = classContent.replaceAll(
            String.format(
                "\\b\\s*=\\s*new\\s*%sServiceImpl\\(\\)",
                featureName
            ),
            ""
        );
        classContent = classContent.replaceAll(
            String.format(
                "((?:(?:public|private|protected)\\s+)?\\s+%s[\\s]*.*?;)(\\n*)",
                serviceField
            ), 
            "$1\n\n" + constructor
        );

        dataModel.put("classContent", classContent);
    }

    private List<String> extractRequiredLibraries(String content) {
        List<String> libraries = new ArrayList<>();
        Pattern pattern = Pattern.compile("import\\s+.*?;");
        Matcher matcher = pattern.matcher(content);
        while (matcher.find()) {
            if (!defaultLibraries.contains(matcher.group()))
                libraries.add(matcher.group());
        }
        return libraries;
    }

    private static String extractClassContent(String content) {
        Pattern pattern = Pattern.compile(
            "(?:(public|private|protected)\\s+)?(?:static\\s+)?\\s+class\\s+\\w+(?:<.*?>)?\\s*.*",
            Pattern.DOTALL
        );
        Matcher matcher = pattern.matcher(content);

        if (matcher.find()) return matcher.group();
        return "";
    }    
}
