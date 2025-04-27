package de.ovgu.featureide.core.winvmj.microservicepreprocessor;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.BlockStmt;

import java.util.Map;
import java.util.Set;

public class RoutingMapAdder {

	public static void addRoutingMap(CompilationUnit cu, Map<String, Set<String>> serviceToEndpointsMap) {
        String methodName = "initRoutingMap";
		
		cu.findAll(MethodDeclaration.class).stream()
                .filter(m -> m.getNameAsString().equals(methodName))
                .findFirst()
                .ifPresent(method -> {
                    BlockStmt body = method.getBody().orElseGet(() -> {
                        BlockStmt newBody = new BlockStmt();
                        method.setBody(newBody);
                        return newBody;
                    });

                    for (Map.Entry<String, Set<String>> entry : serviceToEndpointsMap.entrySet()) {
                        String service = entry.getKey();
                        Set<String> endpoints = entry.getValue();
                        for (String endpoint : endpoints) {
                            MethodCallExpr putCall = new MethodCallExpr(
                                    new NameExpr("routeMap"),
                                    "put"
                            );
                            putCall.addArgument(new StringLiteralExpr(endpoint));
                            putCall.addArgument(new StringLiteralExpr(service + "_URL"));
                            body.addStatement(putCall);
                        }
                    }
                });
    }
}


