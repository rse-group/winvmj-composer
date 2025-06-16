package de.ovgu.featureide.core.winvmj.microservicepreprocessor;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;

import java.util.Optional;

public class ClassMover {

    public static void moveClass(CompilationUnit sourceCU, CompilationUnit targetCU) {
        Optional<ClassOrInterfaceDeclaration> maybeClass = sourceCU.findFirst(ClassOrInterfaceDeclaration.class);
        if (maybeClass.isEmpty()) {
            System.err.println("No classes found in sourceCU.");
            return;
        }

        ClassOrInterfaceDeclaration classToMove = maybeClass.get();

        targetCU.addType(classToMove.clone());

        for (ImportDeclaration importDecl : sourceCU.getImports()) {
            if (!targetCU.getImports().contains(importDecl)) {
                targetCU.addImport(importDecl);
            }
        }
    }
}
