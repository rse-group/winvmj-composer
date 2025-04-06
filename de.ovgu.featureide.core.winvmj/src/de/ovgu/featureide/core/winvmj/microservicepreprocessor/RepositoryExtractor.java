package de.ovgu.featureide.core.winvmj.microservicepreprocessor;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;

import java.io.InputStream;
import java.util.*;

public class RepositoryExtractor {

    private static Map<String, String> extractRepositories(IFolder moduleDir) {
        Map<String, String> repositoryMap = new HashMap<>();
        try {
            for (IResource resource : moduleDir.members()) {
                if (resource instanceof IFolder folder) {
                    if (folder.getName().equals("service")) {
                        extractRepositoriesFromServiceDir(folder, repositoryMap);
                    } else {
                        repositoryMap.putAll(extractRepositories(folder));
                    }
                }
            }
        } catch (CoreException e) {
            e.printStackTrace();
        }
        return repositoryMap;
    }

    private static void extractRepositoriesFromServiceDir(IFolder serviceDir, Map<String, String> repositoryMap) {
        try {
            for (IResource resource : serviceDir.members()) {
                if (resource instanceof IFile file && file.getFileExtension().equals("java")) {
                    parseJavaFile(file, repositoryMap);
                }
            }
        } catch (CoreException e) {
            e.printStackTrace();
        }
    }

    private static void parseJavaFile(IFile file, Map<String, String> repositoryMap) {
    	CompilationUnit cu = JavaParserUtil.parse(file);

        cu.findAll(ClassOrInterfaceDeclaration.class).forEach(classDecl -> {
            classDecl.findAll(FieldDeclaration.class).forEach(field -> {
                if (field.getElementType() instanceof ClassOrInterfaceType type) {
                    if (type.getNameAsString().equals("RepositoryUtil") && type.getTypeArguments().isPresent()) {
                        String domainInterface = type.getTypeArguments().get().get(0).toString();
                        String repositoryName = field.getVariable(0).getNameAsString();

                        Optional<String> modelComponent = findModelComponent(classDecl, repositoryName);
                        modelComponent.ifPresent(component -> {
                            if (component.endsWith(".class")) {
                                component = component.substring(0, component.length() - 6);
                            }
                            repositoryMap.put(domainInterface, component);
                        });
                    }
                }
            });
        });
    }

    private static Optional<String> findModelComponent(ClassOrInterfaceDeclaration classDecl, String repositoryName) {
        return classDecl.getConstructors().stream()
                .flatMap(constructor -> constructor.findAll(AssignExpr.class).stream())
                .filter(assign -> assign.getTarget().toString().equals(repositoryName)
                        || assign.getTarget().toString().equals("this." + repositoryName)
                        && assign.getValue() instanceof ObjectCreationExpr)
                .map(assign -> (ObjectCreationExpr) assign.getValue())
                .filter(init -> init.getType().getNameAsString().equals("RepositoryUtil") && !init.getArguments().isEmpty())
                .map(init -> init.getArgument(0).toString())
                .findFirst();
    }

    public static void initializeRepositoryMap(Set<IFolder> moduleDirs, CompilationUnit cu) {
        Map<String, String> repositoryMap = new HashMap<>();

        for (IFolder moduleDir : moduleDirs) {
            repositoryMap.putAll(extractRepositories(moduleDir));
        }

        cu.findAll(ClassOrInterfaceDeclaration.class).stream()
                .findFirst().flatMap(classDecl -> classDecl.getConstructors().stream().findFirst()).ifPresent(constructor -> {
                    repositoryMap.forEach((domain, component) -> {
                        // Add repositoryMap.put(domain, new RepositoryUtil<>(component));
                        MethodCallExpr putCall = new MethodCallExpr(new NameExpr("repositoryMap"), "put");
                        putCall.addArgument(new StringLiteralExpr(domain));
                        Expression repoInstance = new ObjectCreationExpr(
                                null,
                                new ClassOrInterfaceType(null, "RepositoryUtil")
                                        .setTypeArguments(new ClassOrInterfaceType(null, domain)),
                                new NodeList<>(new FieldAccessExpr(new NameExpr(component), "class"))
                        );
                        putCall.addArgument(repoInstance);

                        constructor.getBody().addStatement(putCall);

                        ImportDeclaration importDecl = new ImportDeclaration(component, false, false);
                        if (!cu.getImports().contains(importDecl)) {
                            cu.addImport(importDecl);
                        }
                    });
                });
    }
}
