package de.ovgu.featureide.core.winvmj.microservicepreprocessor.publish;

import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.*;

import java.util.HashMap;
import java.util.Map;

public class UpdateObjectTrigger extends PublishMessageTrigger{
    public UpdateObjectTrigger() {
        super();
        this.repositoryOperation = "updateObject";
    }

    @Override
    protected Map<String, String> getRequiredAttributes(MethodDeclaration method,
                                                        String objectDomainVar,
                                                        Map<String, String> variableTypeMap) {

        Map<String, String> requiredAttributes = new HashMap<>();
        // Ambil argumen dari method setter untuk objectDomainVar
        method.findAll(MethodCallExpr.class).forEach(call -> {
            if (call.getScope().isPresent()) {
                Expression scopeExpr = call.getScope().get();
                String scopeVar = null;

                // Cek apakah scope adalah casting, misalnya ((CatalogImpl) catalogBrand)
                if (scopeExpr instanceof EnclosedExpr enclosedExpr && enclosedExpr.getInner() instanceof CastExpr castExpr) {
                    Expression castTarget = castExpr.getExpression();
                    if (castTarget instanceof NameExpr nameExpr) {
                        scopeVar = nameExpr.getNameAsString(); // Ambil nama variabel asli setelah casting
                    }
                } else if (scopeExpr instanceof NameExpr nameExpr) {
                    scopeVar = nameExpr.getNameAsString(); // Jika tidak ada casting, gunakan nama variabel langsung
                }

                // Jika scopeVar sesuai dengan objectDomainVar, maka cek apakah ini pemanggilan setter
                if (scopeVar != null && scopeVar.equals(objectDomainVar)) {
                    String methodName = call.getNameAsString();
                    if (methodName.startsWith("set") && call.getArguments().size() == 1) {
                        Expression arg = call.getArgument(0);
                        String argName = arg.toString();
                        String type;

                        // Cek apakah argumen adalah literal (primitif atau string)
                        if (arg.isBooleanLiteralExpr()) {
                            type = "boolean";
                        } else if (arg.isIntegerLiteralExpr()) {
                            type = "int";
                        } else if (arg.isDoubleLiteralExpr()) {
                            type = "double";
                        } else if (arg.isStringLiteralExpr()) {
                            type = "String";
                        } else {
                            // Jika bukan literal, cari dalam daftar deklarasi variabel
                            type = variableTypeMap.getOrDefault(argName, "Unknown");
                        }

                        requiredAttributes.put(argName, type);
                    }
                }
            }
        });

        return requiredAttributes;
    }


}


