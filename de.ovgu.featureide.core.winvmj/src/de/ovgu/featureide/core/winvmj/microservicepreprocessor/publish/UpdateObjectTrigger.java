package de.ovgu.featureide.core.winvmj.microservicepreprocessor.publish;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.Statement;

import java.util.Optional;

public class UpdateObjectTrigger extends PublishMessageTrigger{
    public UpdateObjectTrigger() {
        super();
        this.repositoryOperation = "updateObject";
    }

    @Override
    protected void collectProperties(MethodDeclaration method, String objectModelVar) {
        // Get arguments from the setter method for objectModelVar
        method.findAll(MethodCallExpr.class).forEach(call -> {
            if (call.getScope().isPresent()) {
                Expression scopeExpr = call.getScope().get();
                String scopeVar = null;

                // Check if the scope is casting, for example ((CatalogImpl) catalogBrand)
                if (scopeExpr instanceof EnclosedExpr enclosedExpr && enclosedExpr.getInner() instanceof CastExpr castExpr) {
                    Expression castTarget = castExpr.getExpression();
                    if (castTarget instanceof NameExpr nameExpr) {
                        scopeVar = nameExpr.getNameAsString(); // Take the original variable name after casting
                    }
                } else if (scopeExpr instanceof NameExpr nameExpr) {
                    scopeVar = nameExpr.getNameAsString(); 
                }

                if (scopeVar != null && scopeVar.equals(objectModelVar)) {
                    String methodName = call.getNameAsString();
                    if (methodName.startsWith("set") && call.getArguments().size() == 1) {
                        Expression arg = call.getArgument(0);
                        String fieldName = Character.toLowerCase(methodName.charAt(3)) + methodName.substring(4);
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

                        Statement addToListStmt = createPropertyAddStatement(fieldName,fieldType, fieldValue);

                        // add statement after setter method
                        Optional<Node> stmtNode = call.findAncestor(Statement.class).map(s -> s);
                        stmtNode.ifPresent(stmt -> {
                            stmt.findAncestor(BlockStmt.class).ifPresent(block -> {
                                int index = block.getStatements().indexOf(stmt);
                                if (index != -1) {
                                    block.getStatements().add(index + 1, addToListStmt);
                                }
                            });
                        });


                    }
                }
            }
        });
    }


}


