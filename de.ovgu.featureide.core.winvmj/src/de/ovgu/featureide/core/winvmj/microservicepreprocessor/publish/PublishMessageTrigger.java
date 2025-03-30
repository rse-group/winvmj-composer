package de.ovgu.featureide.core.winvmj.microservicepreprocessor.publish;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.type.ClassOrInterfaceType;

import java.util.*;

public abstract class PublishMessageTrigger {
    String repositoryOperation;
    protected static Set<String> modifiedMethods = new HashSet<>();

    public static void clearModifiedMethods() {modifiedMethods.clear();}

    public void addPublishMessageCall(RepositoryCallInfo repositoryCallInfo, Set<String> domainInterfaces) {
        String domainInterface = repositoryCallInfo.domainInterface();
        String objectDomainVar = repositoryCallInfo.domainObjectVar();
        String repositoryOperation = repositoryCallInfo.repositoryOperation();
        MethodDeclaration method = repositoryCallInfo.method();

        if (! this.repositoryOperation.equals(repositoryOperation)) {
            System.out.println("Repository operation " + repositoryOperation + " is not supported");
            return;
        }
        Map<String, String> variabelTypeMap = getVariableTypeMap(method);

        Map<String, String> requiredAttributes = getRequiredAttributes(method, objectDomainVar, variabelTypeMap);
        addPublishCallStatement(method, objectDomainVar, domainInterface, requiredAttributes, domainInterfaces, repositoryOperation);
    }

    protected abstract Map<String,String> getRequiredAttributes(MethodDeclaration method,
                                                                String objectDomainVar,
                                                                Map<String, String> variableTypeMap);

    protected Map<String,String> getVariableTypeMap(MethodDeclaration method) {
        Map<String, String> variableTypeMap = new HashMap<>();

        // Parameter variable
        method.getParameters().forEach(param -> {
            variableTypeMap.put(param.getNameAsString(), param.getTypeAsString());
        });

        // Method body variable
        method.findAll(VariableDeclarator.class).forEach(varDecl -> {
            variableTypeMap.put(varDecl.getNameAsString(), varDecl.getTypeAsString());
        });

        return variableTypeMap;
    }

    protected void addPublishCallStatement(MethodDeclaration method,
                                                 String objectDomainVar,
                                                 String domainInterface,
                                                 Map<String, String> domainInterfaceVariables,
                                                 Set<String> domainInterfaces,
                                                 String repositoryOperation) {
    	// Membuat instance record Property
        NodeList<Expression> propertiesList = new NodeList<>();
        if (domainInterfaceVariables != null) {
            for (Map.Entry<String, String> entry : domainInterfaceVariables.entrySet()) {
                String varName = entry.getKey();
                String varType = entry.getValue();

                Expression valueExpression;

                // Jika varType ada dalam domainInterfaces, ambil UUID dengan memanggil method get<DomainInterface>Id()
                if (domainInterfaces.contains(varType)) {
                    valueExpression = new MethodCallExpr(new NameExpr(varName), "get" + varType + "Id");
                    varType = "UUID";
                } else if (varType.equals("fqn")) {
                    valueExpression = new NameExpr(varName);
                    varName = "fqn";
                    varType = "String";
                }else {
                    valueExpression = new NameExpr(varName);
                }

                ObjectCreationExpr propertyInstance = new ObjectCreationExpr()
                        .setType("Property")
                        .addArgument(new StringLiteralExpr(varName))
                        .addArgument(new StringLiteralExpr(varType))
                        .addArgument(valueExpression);

                propertiesList.add(propertyInstance);
            }
        }

        // Membuat pemanggilan method <objectDomainVar>.get<DomainInterface>Id()
        MethodCallExpr getIdCall = new MethodCallExpr(
                new NameExpr(objectDomainVar), 
                "get" + domainInterface + "Id"     
        );

        List<Expression> newExpressionList = new ArrayList<>();

        String objectIdVar;
        if (objectDomainVar.toLowerCase().contains("id")) {
            objectIdVar = objectDomainVar; // UUID
        } else {
            objectIdVar = objectDomainVar + "Id";
            VariableDeclarator idVar = new VariableDeclarator(
                    StaticJavaParser.parseClassOrInterfaceType("UUID"),
                    objectIdVar,
                    getIdCall 
            );
            VariableDeclarationExpr idVarDecl = new VariableDeclarationExpr(idVar);
            newExpressionList.add(idVarDecl);
        }

        // Buat objek SteteTransferMessage
        String action;
        if (repositoryOperation.equals("saveObject")) {
            action = "create";
        } else if (repositoryOperation.equals("updateObject")) {
            action = "update";
        } else {
            action = "delete";
        }

        
        MethodCallExpr propertiesVararg = new MethodCallExpr("List.of");
        propertiesList.forEach(propertiesVararg::addArgument);

        if (!modifiedMethods.contains(method.getNameAsString())) {
            // Buat deklarasi variable message
            VariableDeclarator messageVar = new VariableDeclarator(
                    new ClassOrInterfaceType(null, "StateTransferMessage"), // Tipe variabel
                    "message",
                    new ObjectCreationExpr(
                            null,
                            new ClassOrInterfaceType(null, "StateTransferMessage"),
                            NodeList.nodeList(
                                    new NameExpr(objectIdVar),
                                    new StringLiteralExpr(domainInterface),
                                    new StringLiteralExpr(action),
                                    new StringLiteralExpr(getTableName(method, objectDomainVar)),
                                    propertiesVararg
                            )
                    )
            );
            VariableDeclarationExpr messageDeclaration = new VariableDeclarationExpr(messageVar);
            newExpressionList.add(messageDeclaration);
        } else {
            // buat assignment ulang variable message
            AssignExpr reassignMessage = new AssignExpr(
                    new NameExpr("message"),
                    new ObjectCreationExpr(
                            null,
                            new ClassOrInterfaceType(null, "StateTransferMessage"),
                            NodeList.nodeList(
                                    new NameExpr(objectIdVar),
                                    new StringLiteralExpr(domainInterface),
                                    new StringLiteralExpr(action),
                                    new StringLiteralExpr(getTableName(method, objectDomainVar)),
                                    propertiesVararg
                            )
                    ),
                    AssignExpr.Operator.ASSIGN
            );
            newExpressionList.add(reassignMessage);
        }

        // Panggil publishMessage
        MethodCallExpr getInstanceCall = new MethodCallExpr(new NameExpr("RabbitMQManager"), "getInstance");
        MethodCallExpr publishCall = new MethodCallExpr(getInstanceCall, "publishMessage");
        publishCall.addArgument(new NameExpr("routingKey"));
        publishCall.addArgument(new NameExpr("message"));
        newExpressionList.add(publishCall);

        method.findAll(MethodCallExpr.class).forEach(methodCall -> {
            if (methodCall.getNameAsString().equals(repositoryOperation) &&
                    methodCall.getScope().isPresent()) {

                String scopeString = methodCall.getScope().get().toString();
                if (scopeString.endsWith("Repository")) {
                    boolean hasMatchingArgument = methodCall.getArguments().stream()
                            .anyMatch(arg -> arg.toString().equals(objectDomainVar));

                    if (hasMatchingArgument) {
                        methodCall.findAncestor(BlockStmt.class).ifPresent(blockStmt -> {
                            NodeList<Statement> statements = blockStmt.getStatements();
                            int index = -1;

                            // Cari index statement repository call
                            for (int i = 0; i < statements.size(); i++) {
                                if (statements.get(i).toString().contains(methodCall.toString())) {
                                    index = i;
                                    break;
                                }
                            }

                            if (index != -1) {
                                // Sisipkan statement setelah repository call
                                for (int j = newExpressionList.size() - 1; j >= 0; j--) {
                                    statements.add(index + 1, new ExpressionStmt(newExpressionList.get(j)));
                                }
                            }
                        });
                    }
                }
            }
        });
        modifiedMethods.add(method.getNameAsString());
    }
    
    // Untuk mengakses objek delta menggunakan repository call getListObject
    private String getTableName(MethodDeclaration method, String objectDomainVar) {
        for (VariableDeclarator varDecl : method.findAll(VariableDeclarator.class)) {
            if (varDecl.getNameAsString().equals(objectDomainVar)) {
                Expression initializer = varDecl.getInitializer().orElse(null);

                if (initializer instanceof MethodCallExpr callExpr) {
                    while (callExpr.getScope().isPresent() && callExpr.getScope().get() instanceof MethodCallExpr) {
                        callExpr = (MethodCallExpr) callExpr.getScope().get();
                    }

                    Optional<Expression> scopeOpt = callExpr.getScope();

                    if (scopeOpt.isPresent()) {
                        Expression scope = scopeOpt.get();

                        while (scope instanceof MethodCallExpr methodScope) {
                            scope = methodScope.getScope().orElse(null);
                            if (scope == null) {
                                break;
                            }
                        }

                        if (scope instanceof NameExpr nameExpr && nameExpr.getNameAsString().endsWith("Repository")) {
                            if (callExpr.getNameAsString().equals("getListObject") && !callExpr.getArguments().isEmpty()) {
                                Expression firstArg = callExpr.getArgument(0);

                                if (firstArg instanceof StringLiteralExpr stringLiteral) {
                                    return stringLiteral.getValue();
                                }
                            }
                        }
                    }
                }
            }
        }

        return "";
    }

}



