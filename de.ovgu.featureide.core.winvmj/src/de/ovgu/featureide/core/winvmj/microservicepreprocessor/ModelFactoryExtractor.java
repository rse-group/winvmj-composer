package de.ovgu.featureide.core.winvmj.microservicepreprocessor;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.IfStmt;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class ModelFactoryExtractor 	{

    private static Map<String, String> extractFactories(Set<IFolder> moduleDirs) {
        Set<String> modelInterfacesFqn = ModelLayerExtractor.extractModelInterfacesFqn(moduleDirs);
        Set<String> modelInterfaces = modelInterfacesFqn.stream()
                .map(fqn -> fqn.substring(fqn.lastIndexOf('.') + 1))
                .collect(Collectors.toSet());

        Map<String, String> factoryMap = new HashMap<>();
        for (IFolder moduleDir : moduleDirs) {
            factoryMap.putAll(extractFactoriesFromModule(moduleDir, modelInterfaces));
        }
        return factoryMap;
    }

    private static Map<String, String> extractFactoriesFromModule(IFolder moduleDir, Set<String> modelInterfaces) {
        Map<String, String> factoryMap = new HashMap<>();
        try {
            for (IResource resource : moduleDir.members()) {
                if (resource instanceof IFolder) {
                    factoryMap.putAll(extractFactoriesFromModule((IFolder) resource, modelInterfaces));
                } else if (resource instanceof IFile && resource.getName().endsWith("Factory.java")) {
                    parseFactoryFile((IFile) resource, modelInterfaces, factoryMap);
                }
            }
        } catch (CoreException e) {
            e.printStackTrace();
        }

        return factoryMap;
    }

    private static void parseFactoryFile(IFile file, Set<String> modelInterfaces, Map<String, String> factoryMap) {
        String fileName = file.getName();
        for (String model : modelInterfaces) {
            if (fileName.equals(model + "Factory.java")) {
                try {
                    CompilationUnit cu = StaticJavaParser.parse(file.getContents());
                    cu.findAll(ClassOrInterfaceDeclaration.class).forEach(decl -> {
                        if (!decl.isInterface()) {
                            decl.getFullyQualifiedName().ifPresent(fqn -> factoryMap.put(model, fqn));
                        }
                    });
                } catch (CoreException e) {
                    System.err.println("Failed to parse factory file: " + file.getFullPath());
                }
            }
        }
    }

    public static void initializeObjectFactory(Set<IFolder> moduleDirs, CompilationUnit cu) {
        Map<String, String> factoryMap = extractFactories(moduleDirs);

        cu.findAll(ClassOrInterfaceDeclaration.class).stream()
                .filter(classDecl -> !classDecl.getMethodsByName("createObjectHandler").isEmpty())
                .findFirst().flatMap(classDecl -> classDecl.getMethodsByName("createObjectHandler").get(0).getBody()).ifPresent(body -> {
                    IfStmt firstIfStmt = null;
                    IfStmt previousIfStmt = null;

                    for (Map.Entry<String, String> entry : factoryMap.entrySet()) {
                        String model = entry.getKey();
                        String factoryFqn = entry.getValue();
                        String factory = factoryFqn.substring(factoryFqn.lastIndexOf('.') + 1);

                        String objectCreation = model + " obj = " + factory + ".create" + model + "(fqn, arguments.toArray());";
                        String repoSave = "repositoryMap.get(\"" + model + "\").saveObject(obj);";

                        IfStmt ifStmt = new IfStmt(
                        	    new MethodCallExpr(new NameExpr("modelInterface"), "equals", NodeList.nodeList(new StringLiteralExpr(model))),
                        	    new BlockStmt(NodeList.nodeList(
                        	        StaticJavaParser.parseStatement(objectCreation),
                        	        StaticJavaParser.parseStatement(repoSave)
                        	    )),
                        	    null
                        	);

                        if (firstIfStmt == null) {
                            firstIfStmt = ifStmt;
                        } else {
                            previousIfStmt.setElseStmt(ifStmt);
                        }
                        previousIfStmt = ifStmt;

                        // Import statement
                        ImportDeclaration importDecl = new ImportDeclaration(factoryFqn, false, false);
                        if (!cu.getImports().contains(importDecl)) {
                            cu.addImport(importDecl);
                        }
                    }

                    if (firstIfStmt != null) {
                        body.addStatement(firstIfStmt);
                    }
                });
    }
}
