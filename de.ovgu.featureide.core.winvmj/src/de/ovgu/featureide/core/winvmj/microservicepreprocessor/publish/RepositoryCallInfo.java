package de.ovgu.featureide.core.winvmj.microservicepreprocessor.publish;

import com.github.javaparser.ast.body.MethodDeclaration;

public record RepositoryCallInfo(
        MethodDeclaration method,
        String domainInterface,
        String domainObjectVar,
        String repositoryOperation
) { }
