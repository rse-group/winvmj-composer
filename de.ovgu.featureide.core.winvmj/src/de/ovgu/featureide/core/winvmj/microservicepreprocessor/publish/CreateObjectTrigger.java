package de.ovgu.featureide.core.winvmj.microservicepreprocessor.publish;

import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.Statement;

import java.util.*;

public class CreateObjectTrigger extends PublishMessageTrigger{
    public CreateObjectTrigger() {
        super();
        this.repositoryOperation = "saveObject";
    }

    @Override
    protected void collectProperties(MethodDeclaration method, String objectDomainVar){
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

        if (objectDomainDeclaration != null) {
            MethodCallExpr factoryCall = (MethodCallExpr) objectDomainDeclaration.getVariable(0).getInitializer().orElse(null);
            if (factoryCall != null) {
                Optional<Statement> stmtOpt = factoryCall.findAncestor(Statement.class);
                Optional<BlockStmt> blockOpt = factoryCall.findAncestor(BlockStmt.class);

                if (stmtOpt.isPresent() && blockOpt.isPresent()) {
                    Statement stmt = stmtOpt.get();
                    BlockStmt block = blockOpt.get();
                    int index = block.getStatements().indexOf(stmt);

                    List<Statement> toInsert = new ArrayList<>();

                    Expression fqn = factoryCall.getArgument(0);
                    toInsert.add(createPropertyAddStatement("fqn", "String", fqn.toString()));

                    NodeList<Expression> factoryArgs= factoryCall.getArguments();

                    for (int i = 1; i < factoryArgs.size(); i++) {
                        Expression arg = factoryArgs.get(i);
                        String fieldValue = arg.toString();
                        String fieldType;

                        if (arg.isBooleanLiteralExpr()) {
                            fieldType = "boolean";
                        } else if (arg.isIntegerLiteralExpr()) {
                            fieldType = "int";
                        } else if (arg.isDoubleLiteralExpr()) {
                        	String raw = arg.asDoubleLiteralExpr().getValue();
                            if (raw.endsWith("f") || raw.endsWith("F")) {
                                fieldType = "float";
                            } else {
                                fieldType = "double";
                            }
                        } else if (arg.isStringLiteralExpr()) {
                            fieldType = "String";
                        } else {
                            fieldType = variableTypeMap.getOrDefault(fieldValue, "Unknown");
                        }

                        toInsert.add(createPropertyAddStatement(fieldValue, fieldType, fieldValue));
                    }

                    // to maintain order
                    for (int i = toInsert.size() - 1; i >= 0; i--) {
                        block.addStatement(index + 1, toInsert.get(i));
                    }
                }
            }
        }

    }

}


