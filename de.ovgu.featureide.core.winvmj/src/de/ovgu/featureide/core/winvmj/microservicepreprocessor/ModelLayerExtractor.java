package de.ovgu.featureide.core.winvmj.microservicepreprocessor;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;

import java.util.HashSet;
import java.util.Set;

public class ModelLayerExtractor {
	
	public static Set<String> extractModelInterfacesFqn(Set<IFolder> moduleDirs){
		return extractModelsFqn(moduleDirs, "interface");
	}
	
	public static Set<String> extractModelImplementationsFqn(Set<IFolder> moduleDirs){
		return extractModelsFqn(moduleDirs, "implementation");
	}

    private static Set<String> extractModelsFqn(Set<IFolder> moduleDirs, String modelType) {
        Set<String> domainModel = new HashSet<>();

        for (IFolder moduleDir : moduleDirs) {
        	domainModel.addAll(extractModelFqn(moduleDir, modelType));
        }
        return domainModel;
    }

    private static Set<String> extractModelFqn(IFolder moduleDir, String modelType) {
        Set<String> models= new HashSet<>();
        try {
            for (IResource resource : moduleDir.members()) {
                if (resource instanceof IFolder) {
                    IFolder folder = (IFolder) resource;
                    if (folder.getName().equals("model")) {
                    	if (modelType.equals("interface")) {
                    		models.addAll(getModelInterfacesFqn(folder));
                    	} else if (modelType.equals("implementation")){
                    		models.addAll(getModelImplementationsFqn(folder));
                    	}
                        
                    } else {
                        models.addAll(extractModelFqn(folder,modelType));
                    }
                }
            }
        } catch (CoreException e) {
            e.printStackTrace();
        }
        return models;
    }

    private static Set<String> getModelInterfacesFqn(IFolder modelDir) {
        Set<String> result = new HashSet<>();
        try {
            for (IResource resource : modelDir.members()) {
            	if (resource instanceof IFile file && "java".equals(file.getFileExtension())) {
            		CompilationUnit cu = JavaParserUtil.parse(file);
                    
                    cu.findAll(ClassOrInterfaceDeclaration.class).forEach(declaration -> {
                        if (declaration.isInterface()) {
                            declaration.getFullyQualifiedName().ifPresent(result::add);
                        }
                    });
                }
            }
        } catch (CoreException e) {
            e.printStackTrace();
        }
        return result;
    }
    
    
    private static Set<String> getModelImplementationsFqn(IFolder modelDir) {
        Set<String> result = new HashSet<>();
        try {
            for (IResource resource : modelDir.members()) {
                if (resource instanceof IFile file && "java".equals(file.getFileExtension())) {
                    CompilationUnit cu = JavaParserUtil.parse(file);

                    cu.findAll(ClassOrInterfaceDeclaration.class).forEach(declaration -> {
                        if (!declaration.isInterface()
                                && declaration.isPublic()
                                && declaration.getNameAsString().endsWith("Impl")) {
                            declaration.getFullyQualifiedName().ifPresent(result::add);
                        }
                    });
                }
            }
        } catch (CoreException e) {
            e.printStackTrace();
        }
        return result;
    }
}
