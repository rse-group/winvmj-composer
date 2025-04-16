package de.ovgu.featureide.core.winvmj.microservicepreprocessor;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.IfStmt;

import java.util.Set;

public class AttributeSetterModifier {

    public static void modifySetAttributesMethod(CompilationUnit cu, Set<String> modelImplFqns) {
        cu.findAll(MethodDeclaration.class).stream()
                .filter(method -> method.getNameAsString().equals("setAttributes"))
                .findFirst()
                .ifPresent(method -> {
                    BlockStmt body = method.getBody().orElseGet(BlockStmt::new);

                    IfStmt firstIfStmt = null;
                    IfStmt previousIfStmt = null;

                    for (String fqn : modelImplFqns) {
                        // Create if (domainClassImpl.equals("..."))
                        Expression condition = new MethodCallExpr(new NameExpr("domainClassImpl"), "equals")
                                .addArgument(new StringLiteralExpr(fqn));

                        // Create body: obj = (<FQN>) obj;
                        ExpressionStmt assignStmt = new ExpressionStmt(
                                new AssignExpr(
                                        new NameExpr("obj"),
                                        new CastExpr(
                                                StaticJavaParser.parseClassOrInterfaceType(fqn),
                                                new NameExpr("obj")
                                        ),
                                        AssignExpr.Operator.ASSIGN
                                )
                        );

                        IfStmt ifStmt = new IfStmt(condition, new BlockStmt().addStatement(assignStmt), null);

                        if (firstIfStmt == null) {
                            firstIfStmt = ifStmt;
                        } else {
                            previousIfStmt.setElseStmt(ifStmt);
                        }
                        previousIfStmt = ifStmt;
                    }

                    if (firstIfStmt != null) {
                        body.addStatement(0, firstIfStmt);
                        method.setBody(body);
                    }
                });
    }
}


