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

    public static Set<String> extractModelInterfacesFqn(Set<IFolder> moduleDirs) {
        Set<String> domainModelInterfaces = new HashSet<>();

        for (IFolder moduleDir : moduleDirs) {
            domainModelInterfaces.addAll(extractModelInterfacesFqn(moduleDir));
        }
        return domainModelInterfaces;
    }

    public static Set<String> extractModelInterfacesFqn(IFolder moduleDir) {
        Set<String> domainInterfaces = new HashSet<>();
        try {
            for (IResource resource : moduleDir.members()) {
                if (resource instanceof IFolder) {
                    IFolder folder = (IFolder) resource;
                    if (folder.getName().equals("model")) {
                        domainInterfaces.addAll(getModelInterfacesFqn(folder));
                    } else {
                        domainInterfaces.addAll(extractModelInterfacesFqn(folder));
                    }
                }
            }
        } catch (CoreException e) {
            e.printStackTrace();
        }
        return domainInterfaces;
    }

    private static Set<String> getModelInterfacesFqn(IFolder modelDir) {
        Set<String> result = new HashSet<>();
        try {
            for (IResource resource : modelDir.members()) {
                if (resource instanceof IFile) {
                    IFile file = (IFile) resource;
                    if (file.getFileExtension().equals("java")) {
                        CompilationUnit cu = JavaParserUtil.parse(file);
                        
                        cu.findAll(ClassOrInterfaceDeclaration.class).forEach(declaration -> {
                            if (declaration.isInterface()) {
                                declaration.getFullyQualifiedName().ifPresent(result::add);
                            }
                        });
                    }
                }
            }
        } catch (CoreException e) {
            e.printStackTrace();
        }
        return result;
    }

    public static void addModelInterfaceImportStatement(Set<IFolder> moduleDirs, CompilationUnit cu) {
        Set<String> domainModelInterfaces = extractModelInterfacesFqn(moduleDirs);
        Set<String> existingImports = new HashSet<>();
        cu.getImports().forEach(importDecl -> existingImports.add(importDecl.getNameAsString()));

        for (String importStr : domainModelInterfaces) {
            if (!existingImports.contains(importStr)) {
                cu.addImport(importStr);
            }
        }
    }
}
