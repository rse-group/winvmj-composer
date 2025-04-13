package de.ovgu.featureide.core.winvmj.microservicepreprocessor;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.NodeList;

import java.util.Optional;

public class MessageConsumerRegister {

    public static void addMessageConsumerRegistration(CompilationUnit cu) {
        Optional<ClassOrInterfaceDeclaration> maybeClass = cu.findFirst(ClassOrInterfaceDeclaration.class);
        if (maybeClass.isEmpty()) return;

        ClassOrInterfaceDeclaration classDecl = maybeClass.get();

        Optional<MethodDeclaration> maybeMainMethod = classDecl.getMethodsByName("main").stream()
                .filter(m -> m.isStatic() && m.getParameters().size() == 1 && m.getParameter(0).getTypeAsString().equals("String[]"))
                .findFirst();

        if (maybeMainMethod.isEmpty()) {
            System.err.println("No main method found");
            return;
        }

        MethodDeclaration mainMethod = maybeMainMethod.get();
        BlockStmt body = mainMethod.getBody().orElseGet(() -> {
            BlockStmt newBody = new BlockStmt();
            mainMethod.setBody(newBody);
            return newBody;
        });

        // Statement 1: MessageConsumer messageConsumer = new MessageConsumerImpl();
        VariableDeclarationExpr messageConsumerDecl = new VariableDeclarationExpr(
                new ClassOrInterfaceType(null, "MessageConsumer"),
                "messageConsumer"
        );
        ObjectCreationExpr newConsumerExpr = new ObjectCreationExpr(
                null,
                new ClassOrInterfaceType(null, "MessageConsumerImpl"),
                new NodeList<>()
        );
        messageConsumerDecl.getVariable(0).setInitializer(newConsumerExpr);

        // Statement 2: RabbitMQManager.getInstance().addConsumer(messageConsumer);
        MethodCallExpr addConsumerCall = new MethodCallExpr(
                new MethodCallExpr(new NameExpr("RabbitMQManager"), "getInstance"),
                "addMessageConsumer",
                NodeList.nodeList(new NameExpr("messageConsumer"))
        );

        body.addStatement(new ExpressionStmt(messageConsumerDecl));
        body.addStatement(new ExpressionStmt(addConsumerCall));
    }
}

