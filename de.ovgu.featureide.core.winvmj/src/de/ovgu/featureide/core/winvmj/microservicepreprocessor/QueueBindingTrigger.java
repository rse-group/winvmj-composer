package de.ovgu.featureide.core.winvmj.microservicepreprocessor;

import com.github.javaparser.ast.*;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.*;

import java.util.List;
import java.util.Optional;

public class QueueBindingTrigger {
    public static void addQueueBindingCall(CompilationUnit cu) {
        cu.findAll(ClassOrInterfaceDeclaration.class).forEach(cls -> {
            if (!cls.isInterface()) {

                // Add class attribute initialization in constructor
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

                        if (superCall.isPresent()) { // Memastikan statement yang ditambahkan tidak mendahului super() call
                            int superIndex = body.getStatements().indexOf(superCall.get());
                            body.addStatement(superIndex + 1, generateQueueAssignment());
                            body.addStatement(superIndex + 2, generateBindQueueCall());
                        } else {
                            body.addStatement(generateQueueAssignment());
                            body.addStatement(generateBindQueueCall());
                        }
                    });
                } else {
                    ConstructorDeclaration constructor = new ConstructorDeclaration()
                            .setName(cls.getNameAsString())
                            .setModifiers(Modifier.Keyword.PUBLIC);

                    BlockStmt body = new BlockStmt();
                    body.addStatement(generateQueueAssignment());
                    body.addStatement(generateBindQueueCall());

                    constructor.setBody(body);

                    cls.getMembers().add(0, constructor);
                }

                // Add queue attribute
                if (cls.getFieldByName("queue").isEmpty()) {
                    FieldDeclaration fieldDeclaration = new FieldDeclaration()
                            .addVariable(new VariableDeclarator().setType("String").setName("queue"))
                            .setModifiers(Modifier.Keyword.PRIVATE);

                    cls.getMembers().add(0, fieldDeclaration);
                }

                // Add routingKey attribute
                if (cls.getFieldByName("routingKey").isEmpty()) {
                    String packageName = cu.getPackageDeclaration()
                            .map(pd -> pd.getName().asString())
                            .orElse("No_Package");

                    FieldDeclaration packageField = new FieldDeclaration()
                            .addVariable(new VariableDeclarator()
                                    .setType("String")
                                    .setName("routingKey")
                                    .setInitializer("\"" + packageName.replace(".", "_") + "\""))
                            .setModifiers(Modifier.Keyword.PRIVATE);
                    cls.getMembers().add(0,packageField);
                }
            }
        });
    }

    // this.queue = System.getenv("app_id") + "." + routingKey;
    private static ExpressionStmt generateQueueAssignment() {
        MethodCallExpr getenvCall = new MethodCallExpr(new NameExpr("System"), "getenv")
                .addArgument(new StringLiteralExpr("app_id"));
        BinaryExpr appIdWithDot = new BinaryExpr(getenvCall, new StringLiteralExpr("."), BinaryExpr.Operator.PLUS);
        BinaryExpr queueValue = new BinaryExpr(appIdWithDot, new NameExpr("routingKey"), BinaryExpr.Operator.PLUS);
        AssignExpr assignQueue = new AssignExpr(new NameExpr("this.queue"), queueValue, AssignExpr.Operator.ASSIGN);
        return new ExpressionStmt(assignQueue);
    }

    //  RabbitMQManager.getInstance().bindQueue(queue, routingKey);
    private static ExpressionStmt generateBindQueueCall() {
        MethodCallExpr getInstanceCall = new MethodCallExpr(new NameExpr("RabbitMQManager"), "getInstance");
        MethodCallExpr bindQueueCall = new MethodCallExpr(getInstanceCall, "bindQueue");
        bindQueueCall.addArgument(new NameExpr("queue"));
        bindQueueCall.addArgument(new NameExpr("routingKey"));
        return new ExpressionStmt(bindQueueCall);
    }

}
