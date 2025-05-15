package de.ovgu.featureide.core.winvmj.microservicepreprocessor.publish;

import com.github.javaparser.ast.body.MethodDeclaration;

public class DeleteObjectTrigger extends PublishMessageTrigger{
    public DeleteObjectTrigger() {
        super();
        this.repositoryOperation = "deleteObject";
    }

    @Override
    protected void collectProperties(MethodDeclaration method, String objectDomainVar) {}

}


