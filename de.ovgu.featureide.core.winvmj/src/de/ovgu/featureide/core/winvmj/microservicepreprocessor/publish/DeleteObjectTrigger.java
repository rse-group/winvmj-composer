package de.ovgu.featureide.core.winvmj.microservicepreprocessor.publish;

import com.github.javaparser.ast.body.MethodDeclaration;

import java.util.Map;

public class DeleteObjectTrigger extends PublishMessageTrigger{
    public DeleteObjectTrigger() {
        super();
        this.repositoryOperation = "deleteObject";
    }

    @Override
    protected Map<String, String> getRequiredAttributes(MethodDeclaration method,
                                                        String objectDomainVar,
                                                        Map<String, String> variableTypeMap) {
        return null;
    }

}


