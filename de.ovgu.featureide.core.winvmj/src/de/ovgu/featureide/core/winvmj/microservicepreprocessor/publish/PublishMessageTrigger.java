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
    protected static Map<String, Integer> modifiedMethods = new HashMap<>();
    
    protected Map<String,String> variableTypeMap = new HashMap<>();
    protected Set<String> nullInitializedVariables = new HashSet<>();
    protected Set<String> domainInterfaces = new HashSet<>();
    
    protected String propertiesVarName;

    public static void clearModifiedMethods() {modifiedMethods.clear();}

    public void addPublishMessageCall(RepositoryCallInfo repositoryCallInfo, Set<String> domainInterfaces) {
        String domainInterface = repositoryCallInfo.domainInterface();
        String objectDomainVar = repositoryCallInfo.domainObjectVar();
        String repositoryOperation = repositoryCallInfo.repositoryOperation();
        MethodDeclaration method = repositoryCallInfo.method();
        
        this.domainInterfaces = domainInterfaces;
        String methodId = method.getSignature().asString();
        
        propertiesVarName = "properties";
        if (modifiedMethods.getOrDefault(methodId, 0) > 0) {
        	propertiesVarName += String.valueOf(modifiedMethods.get(methodId));
        }
        

        if (! this.repositoryOperation.equals(repositoryOperation)) {
            System.out.println("Repository operation " + repositoryOperation + " is not supported");
            return;
        }
        setVariableTypeMap(method);
        initializePropertiesVar(method);
        collectProperties(method, objectDomainVar);
        addPublishCallStatement(method, objectDomainVar, domainInterface, repositoryOperation);
        
        modifiedMethods.put(methodId, modifiedMethods.getOrDefault(methodId, 0) + 1);
    }

    protected abstract void collectProperties(MethodDeclaration method, String objectDomainVar);

    protected Map<String,String> setVariableTypeMap(MethodDeclaration method) {
        variableTypeMap = new HashMap<>();
        nullInitializedVariables = new HashSet<>();

        // Parameter variable
        method.getParameters().forEach(param -> {
            variableTypeMap.put(param.getNameAsString(), param.getTypeAsString());
        });

        // Method body variable
        method.findAll(VariableDeclarator.class).forEach(varDecl -> {
            if (varDecl.getInitializer().isPresent() && varDecl.getInitializer().get().isNullLiteralExpr()) {
                nullInitializedVariables.add(varDecl.getNameAsString());
            }
            variableTypeMap.put(varDecl.getNameAsString(), varDecl.getTypeAsString());
        });

        return variableTypeMap;
    }
    
    private void initializePropertiesVar(MethodDeclaration method){
        ClassOrInterfaceType listType = StaticJavaParser.parseClassOrInterfaceType("List<Property>");
        Expression initializer = new MethodCallExpr("new ArrayList<>");
        VariableDeclarator variable = new VariableDeclarator(listType, propertiesVarName, initializer);
        VariableDeclarationExpr propertiesDeclaration = new VariableDeclarationExpr(variable);

        method.getBody().ifPresent(body -> body.addStatement(0, propertiesDeclaration));
    }
    
    protected Statement createPropertyAddStatement(String fieldName, String fieldType, String fieldValue) {
    	Expression valueExpression;

        if (domainInterfaces.contains(fieldType)) {
            if (nullInitializedVariables.contains(fieldValue)) {
                valueExpression = new ConditionalExpr(
                        new BinaryExpr(new NameExpr(fieldValue), new NullLiteralExpr(), BinaryExpr.Operator.NOT_EQUALS),
                        new MethodCallExpr(new NameExpr(fieldValue), "get" + fieldType + "Id"),
                        new NullLiteralExpr()
                );
            } else {
                valueExpression = new MethodCallExpr(new NameExpr(fieldValue), "get" + fieldType + "Id");
            }
        } else {
            valueExpression = new NameExpr(fieldValue);
        }

        ObjectCreationExpr propertyExpr = new ObjectCreationExpr()
                .setType("Property")
                .addArgument(new StringLiteralExpr(fieldName))
                .addArgument(new StringLiteralExpr(fieldType))
                .addArgument(valueExpression);

        MethodCallExpr addToList = new MethodCallExpr(new NameExpr(propertiesVarName), "add")
                .addArgument(propertyExpr);

        return new ExpressionStmt(addToList);
    }

    protected void addPublishCallStatement(MethodDeclaration method, 
    		String objectDomainVar, 
    		String domainInterface,
    		String repositoryOperation ) 
    {
    	List<Expression> newExpressionList = new ArrayList<>();
    	
        String objectIdVar;
        if (objectDomainVar.toLowerCase().contains("id")) {
            objectIdVar = objectDomainVar;
        } else {
            objectIdVar = objectDomainVar + "Identifier"; // avoid the same variable name, usually use id
            
            // Create method call <objectDomainVar>.get<DomainInterface>Id()
            MethodCallExpr getIdCall = new MethodCallExpr(new NameExpr(objectDomainVar), "get" + domainInterface + "Id");
            VariableDeclarator idVar = new VariableDeclarator(
                    StaticJavaParser.parseClassOrInterfaceType("Object"),
                    objectIdVar,
                    getIdCall
            );
            VariableDeclarationExpr idVarDecl = new VariableDeclarationExpr(idVar);
            newExpressionList.add(idVarDecl);
        }

        String action = switch (repositoryOperation) {
            case "saveObject" -> "create";
            case "updateObject" -> "update";
            default -> "delete";
        };

        Expression stateTransferMessageExpr = new ObjectCreationExpr(
                null,
                new ClassOrInterfaceType(null, "StateTransferMessage"),
                NodeList.nodeList(
                        new NameExpr(objectIdVar),
                        new StringLiteralExpr(domainInterface),
                        new StringLiteralExpr(action),
                        new StringLiteralExpr(getTableName(method, objectDomainVar)),
                        new NameExpr(propertiesVarName)
                )
        );
        
        String methodId = method.getSignature().asString();
        if (!modifiedMethods.containsKey(methodId)) {
            VariableDeclarator messageVar = new VariableDeclarator(
                    new ClassOrInterfaceType(null, "StateTransferMessage"),
                    "message",
                    stateTransferMessageExpr
            );
            VariableDeclarationExpr messageDecl = new VariableDeclarationExpr(messageVar);
            newExpressionList.add(messageDecl);
        } else {
            AssignExpr reassignMessage = new AssignExpr(
                    new NameExpr("message"),
                    stateTransferMessageExpr,
                    AssignExpr.Operator.ASSIGN
            );
            newExpressionList.add(reassignMessage);
        }

        MethodCallExpr publishCall = new MethodCallExpr(
                new MethodCallExpr(new NameExpr("RabbitMQManager"), "getInstance"),
                "publishMessage"
        );
        publishCall.addArgument(new NameExpr("routingKey"));
        publishCall.addArgument(new NameExpr("message"));
        newExpressionList.add(publishCall);

        method.findAll(MethodCallExpr.class).forEach(methodCall -> {
            if (methodCall.getNameAsString().equals(repositoryOperation) && methodCall.getScope().isPresent()) {
                String scopeString = methodCall.getScope().get().toString();
                if (scopeString.endsWith("Repository")) {
                    boolean hasMatchingArg = methodCall.getArguments().stream()
                            .anyMatch(arg -> arg.toString().equals(objectDomainVar));

                    if (hasMatchingArg) {
                        methodCall.findAncestor(BlockStmt.class).ifPresent(blockStmt -> {
                            NodeList<Statement> statements = blockStmt.getStatements();
                            int index = -1;
                            for (int i = 0; i < statements.size(); i++) {
                                if (statements.get(i).toString().contains(methodCall.toString())) {
                                    index = i;
                                    break;
                                }
                            }
                            if (index != -1) {
                                for (int j = newExpressionList.size() - 1; j >= 0; j--) {
                                    statements.add(index + 1, new ExpressionStmt(newExpressionList.get(j)));
                                }
                            }
                        });
                    }
                }
            }
        });
    }
    
    // To access delta objects use the repository call getListObject
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



