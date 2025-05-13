package de.ovgu.featureide.core.winvmj.microservicepreprocessor;

import com.github.javaparser.ast.*;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.*;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public class QueueBindingTrigger {
	public static void addQueueBindingCall(CompilationUnit cu, Set<String> routingKeys) {
	    cu.findAll(ClassOrInterfaceDeclaration.class).forEach(cls -> {
	        if (!cls.isInterface()) {

	            List<ConstructorDeclaration> constructors = cls.getConstructors();

	            if (!constructors.isEmpty()) {
	                constructors.forEach(constructor -> {
	                    BlockStmt body = constructor.getBody();

	                    Optional<ExplicitConstructorInvocationStmt> superCall = body
	                            .getStatements()
	                            .stream()
	                            .filter(stmt -> stmt instanceof ExplicitConstructorInvocationStmt)
	                            .map(stmt -> (ExplicitConstructorInvocationStmt) stmt)
	                            .findFirst();

	                    int insertIndex = superCall.map(stmt -> body.getStatements().indexOf(stmt) + 1).orElse(-1);

	                    for (String rk : routingKeys) {
	                        ExpressionStmt bindStmt = generateBindQueueCall(rk);
	                        if (insertIndex >= 0) {
	                            body.addStatement(insertIndex++, bindStmt);
	                        } else {
	                            body.addStatement(bindStmt);
	                        }
	                    }
	                });
	            } else {
	                ConstructorDeclaration constructor = new ConstructorDeclaration()
	                        .setName(cls.getNameAsString())
	                        .setModifiers(Modifier.Keyword.PUBLIC);

	                BlockStmt body = new BlockStmt();
	                for (String rk : routingKeys) {
	                    body.addStatement(generateBindQueueCall(rk));
	                }

	                constructor.setBody(body);
	                cls.getMembers().addFirst(constructor);
	            }
	        }
	    });
	}
    
    public static String addRoutingKeyAttribute(CompilationUnit cu) {
        final String[] routingKey = {""};

        cu.findAll(ClassOrInterfaceDeclaration.class).forEach(cls -> {
            if (cls.isInterface() || cls.getFieldByName("routingKey").isPresent()) return;

            String packageName = cu.getPackageDeclaration()
                    .map(pd -> pd.getName().asString())
                    .orElse("No_Package");

            String className = cls.getNameAsString();
            String baseClassName = className.endsWith("ServiceImpl")
                    ? className.substring(0, className.length() - "ServiceImpl".length())
                    : className;

            String routingKeyValue = packageName.replace(".", "_") + "_" + baseClassName.toLowerCase();
            routingKey[0] = routingKeyValue;
            FieldDeclaration routingKeyField = new FieldDeclaration()
                    .addVariable(new VariableDeclarator()
                            .setType("String")
                            .setName("routingKey")
                            .setInitializer("\"" + routingKeyValue + "\""))
                    .setModifiers(Modifier.Keyword.PRIVATE);

            cls.getMembers().addFirst(routingKeyField);
        });

        return routingKey[0];
    }
    
    private static ExpressionStmt generateBindQueueCall(String routingKeyValue) {
        // System.getenv("APP_ID")
        MethodCallExpr getenvCall = new MethodCallExpr(new NameExpr("System"), "getenv")
                .addArgument(new StringLiteralExpr("APP_ID"));

        // System.getenv("APP_ID") + "."
        BinaryExpr appIdWithDot = new BinaryExpr(getenvCall, new StringLiteralExpr("."), BinaryExpr.Operator.PLUS);

        // System.getenv("APP_ID") + "." + "routingKeyValue"
        BinaryExpr fullQueueName = new BinaryExpr(appIdWithDot, new StringLiteralExpr(routingKeyValue), BinaryExpr.Operator.PLUS);

        // RabbitMQManager.getInstance()
        MethodCallExpr getInstanceCall = new MethodCallExpr(new NameExpr("RabbitMQManager"), "getInstance");

        // bindQueue(System.getenv("APP_ID") + "." + "routingKeyValue", "routingKeyValue")
        MethodCallExpr bindQueueCall = new MethodCallExpr(getInstanceCall, "bindQueue");
        bindQueueCall.addArgument(fullQueueName);
        bindQueueCall.addArgument(new StringLiteralExpr(routingKeyValue));

        return new ExpressionStmt(bindQueueCall);
    }

}
