package id.ac.ui.cs.prices.winvmj.composer.microservicepreprocessor.publish;

import com.github.javaparser.ast.body.MethodDeclaration;

public class DeleteObjectTrigger extends PublishMessageTrigger{
    public DeleteObjectTrigger() {
        super();
        this.repositoryOperation = "deleteObject";
    }

    @Override
    protected void collectProperties(MethodDeclaration method, String objectModelVar) {}

}


