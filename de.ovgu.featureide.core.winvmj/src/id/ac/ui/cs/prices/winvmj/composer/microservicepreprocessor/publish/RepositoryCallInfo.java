package id.ac.ui.cs.prices.winvmj.composer.microservicepreprocessor.publish;

import com.github.javaparser.ast.body.MethodDeclaration;

public record RepositoryCallInfo(
        MethodDeclaration method,
        String modelInterface,
        String modelObjectVar,
        String repositoryOperation
) { }
