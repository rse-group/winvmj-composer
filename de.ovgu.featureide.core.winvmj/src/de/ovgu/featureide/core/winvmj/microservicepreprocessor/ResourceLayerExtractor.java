package de.ovgu.featureide.core.winvmj.microservicepreprocessor;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.MemberValuePair;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public class ResourceLayerExtractor {
	
    public static Set<String> extractEndpoints(Set<IFolder> moduleDirs) {
        Set<String> endpoints = new HashSet<>();

        for (IFolder moduleDir : moduleDirs) {
        	endpoints.addAll(extractEndpoints(moduleDir));
        }
        return endpoints;
    }

    public static Set<String> extractEndpoints(IFolder moduleDir) {
        Set<String> endpoints= new HashSet<>();
        try {
            for (IResource resource : moduleDir.members()) {
                if (resource instanceof IFolder) {
                    IFolder folder = (IFolder) resource;
                    if (folder.getName().equals("resource")) {
                    	endpoints.addAll(getResourceEndpoints(folder));
                    } else {
                    	endpoints.addAll(extractEndpoints(folder));
                    }
                }
            }
        } catch (CoreException e) {
            e.printStackTrace();
        }
        return endpoints;
    }
       
    private static Set<String> getResourceEndpoints(IFolder modelDir) {
        Set<String> result = new HashSet<>();
        try {
            for (IResource resource : modelDir.members()) {
                if (resource instanceof IFile file && file.getName().endsWith("ResourceImpl.java")) {
                    CompilationUnit cu = JavaParserUtil.parse(file);

                    result.addAll(extractEndpointUrls(cu));
                }
            }
        } catch (CoreException e) {
            e.printStackTrace();
        }
        return result;
    }
    
    private static Set<String> extractEndpointUrls(CompilationUnit cu) {
        Set<String> urls = new HashSet<>();

        cu.findAll(MethodDeclaration.class).forEach(method -> {
            Optional<AnnotationExpr> routeAnnotation = method.getAnnotationByName("Route");

            routeAnnotation.ifPresent(annotation -> {
                if (annotation.isNormalAnnotationExpr()) {
                    NormalAnnotationExpr normalAnnotation = annotation.asNormalAnnotationExpr();
                    for (MemberValuePair pair : normalAnnotation.getPairs()) {
                        if (pair.getNameAsString().equals("url")) {
                        	String rawUrl = pair.getValue().toString();
                            String cleanedUrl = "/" + rawUrl.replaceAll("^\"|\"$", ""); // Remove surrounding quotes
                            urls.add(cleanedUrl);
                        }
                    }
                }
            });
        });

        return urls;
    }
}
