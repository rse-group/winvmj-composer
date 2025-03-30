package de.ovgu.featureide.core.winvmj.microservicepreprocessor.publish;

import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.*;

import java.util.*;

public class CreateObjectTrigger extends PublishMessageTrigger{
    public CreateObjectTrigger() {
        super();
        this.repositoryOperation = "saveObject";
    }

    @Override
    protected Map<String, String> getRequiredAttributes(MethodDeclaration method,
                                                        String objectDomainVar,
                                                        Map<String, String> variableTypeMap) {

        VariableDeclarationExpr objectDomainDeclaration = null;
        for (VariableDeclarationExpr varExpr : method.findAll(VariableDeclarationExpr.class)) {
            for (VariableDeclarator var : varExpr.getVariables()) {
                if (var.getNameAsString().equals(objectDomainVar)) {
                    objectDomainDeclaration = varExpr;
                    break;
                }
            }
            if (objectDomainDeclaration != null) {
                break;
            }
        }

        Map<String, String> requiredAttributes = new LinkedHashMap<>();

        if (objectDomainDeclaration != null) {
            MethodCallExpr factoryCall = (MethodCallExpr) objectDomainDeclaration.getVariable(0).getInitializer().orElse(null);
            if (factoryCall != null) {
                Expression fqn = factoryCall.getArgument(0);
                requiredAttributes.put(fqn.toString(),"fqn");
                for (Expression arg : factoryCall.getArguments()) {
                    if (arg instanceof NameExpr) {
                        String argName = ((NameExpr) arg).getNameAsString();
                        String type;

                        if (arg.isBooleanLiteralExpr()) {
                            type = "boolean";
                        } else if (arg.isIntegerLiteralExpr()) {
                            type = "int";
                        } else if (arg.isDoubleLiteralExpr()) {
                            type = "double";
                        } else if (arg.isStringLiteralExpr()) {
                            type = "String";
                        } else {
                            type = variableTypeMap.getOrDefault(argName, "Unknown");
                        }

                        requiredAttributes.put(argName, type);
                    }
                }
            }
        }

        return requiredAttributes;
    }

}


