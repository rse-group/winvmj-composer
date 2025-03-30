package de.ovgu.featureide.core.winvmj.microservicepreprocessor.publish;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;

import java.util.*;

public class PublishMessageCallAdder {
    Set<String> domainInterfaces;

    PublishMessageTrigger createObjectTrigger;
    PublishMessageTrigger updateObjectTrigger;
    PublishMessageTrigger deleteObjectTrigger;

    public PublishMessageCallAdder(Set<String> domainInterfaces) {
        this.domainInterfaces = domainInterfaces;
        this.createObjectTrigger = new CreateObjectTrigger();
        this.updateObjectTrigger = new UpdateObjectTrigger();
        this.deleteObjectTrigger = new DeleteObjectTrigger();

    }

    public void addPublishMessageCall(CompilationUnit cu) {
        PublishMessageTrigger.clearModifiedMethods();
        List<RepositoryCallInfo> repositoryCallInfos = getAllRepositoryCall(cu);

        for (RepositoryCallInfo repositoryCallInfo : repositoryCallInfos) {
            switch (repositoryCallInfo.repositoryOperation()) {
                case "saveObject" -> createObjectTrigger.addPublishMessageCall(repositoryCallInfo,domainInterfaces);
                case "updateObject" -> updateObjectTrigger.addPublishMessageCall(repositoryCallInfo,domainInterfaces);
                case "deleteObject" -> deleteObjectTrigger.addPublishMessageCall(repositoryCallInfo,domainInterfaces);
                default -> System.out.println("Invalid repository operation");
            }

            addImportStatement(cu);
        }
    }

    private List<RepositoryCallInfo> getAllRepositoryCall(CompilationUnit cu) {
        List<RepositoryCallInfo> repositoryCallInfos = new ArrayList<>();

        cu.findAll(MethodDeclaration.class).forEach(method -> {
            method.findAll(MethodCallExpr.class).forEach(call -> {
                String repositoryOperation = call.getNameAsString();

                if (repositoryOperation.equals("saveObject") ||
                        repositoryOperation.equals("updateObject") ||
                        repositoryOperation.equals("deleteObject")) {

                    Optional<Expression> firstArgument = call.getArguments().stream().findFirst();
                    String domainInterface = "Unknown";
                    String objectDomainVar = "Unknown";

                    if (call.getScope().isPresent()) {
                        String scopeString = call.getScope().get().toString();

                        if (scopeString.endsWith("Repository")) {
                            // Ambil nama domainInterface dari nama repository (e.g catalogRepository -> Catalog)
                            domainInterface = Character.toUpperCase(scopeString.charAt(0)) + scopeString.substring(1, scopeString.length() - 10);
                        }
                    }

                    if (firstArgument.isPresent()) {
                        Expression argument = firstArgument.get();

                        if (argument.isNameExpr()) {
                            objectDomainVar = argument.asNameExpr().getNameAsString();
                        }
                    }

                    repositoryCallInfos.add(new RepositoryCallInfo(method, domainInterface, objectDomainVar, repositoryOperation));
                }
            });
        });

        return repositoryCallInfos;
    }

    private void addImportStatement(CompilationUnit cu) {
        String messagingModule = "vmj.messaging";
        List<String> requiredImports = List.of(
                "java.util.UUID",
                "java.util.List",
                messagingModule + ".StateTransferMessage",
                messagingModule + ".Property",
                messagingModule + ".rabbitmq.RabbitMQManager"
        );

        requiredImports.forEach(importStr -> {
            ImportDeclaration importDecl = new ImportDeclaration(importStr, false, false);
            if (!cu.getImports().contains(importDecl)) {
                cu.addImport(importDecl);
            }
        });
    }
}
