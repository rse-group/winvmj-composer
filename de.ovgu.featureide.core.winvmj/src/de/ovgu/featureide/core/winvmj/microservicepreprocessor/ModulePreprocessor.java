package de.ovgu.featureide.core.winvmj.microservicepreprocessor;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import de.ovgu.featureide.core.winvmj.microservicepreprocessor.publish.PublishMessageCallAdder;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.CoreException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.stream.Collectors;

public class ModulePreprocessor {

    public static void modifyServiceImplClass(Set<IFolder> moduleDirs) {
        Set<String> domainInterfacesFqn = ModelLayerExtractor.extractModelInterfacesFqn(moduleDirs);
        Set<String> domainInterfaces = domainInterfacesFqn.stream()
                .map(fqn -> fqn.substring(fqn.lastIndexOf('.') + 1))
                .collect(Collectors.toSet());

        PublishMessageCallAdder publishMessageCallAdder = new PublishMessageCallAdder(domainInterfaces);

        for (IFolder moduleDir : moduleDirs) {
            IFile serviceImplFile = getFileByName(moduleDir, "ServiceImpl.java");
            if (serviceImplFile == null) continue;

            CompilationUnit cu = parseJavaFile(serviceImplFile);
            QueueBindingTrigger.addQueueBindingCall(cu);
            publishMessageCallAdder.addPublishMessageCall(cu);

            overwriteFile(serviceImplFile, cu);
        }
    }

    public static void modifyRabbitmqManagerClass(Set<IFolder> moduleDirs, IFolder messagesModuleDir) {
        IFile rabbitmqManagerFile = getFileByName(messagesModuleDir, "RabbitMQManager.java");
        if (rabbitmqManagerFile == null) return;

        CompilationUnit cu = parseJavaFile(rabbitmqManagerFile);
        ModelLayerExtractor.addModelInterfaceImportStatement(moduleDirs, cu);
        ModelFactoryExtractor.initializeObjectFactory(moduleDirs, cu);
        RepositoryExtractor.initializeRepositoryMap(moduleDirs, cu);

        overwriteFile(rabbitmqManagerFile, cu);
    }

    public static void modifyModuleInfo(Set<IFolder> moduleDirs) {
        for (IFolder moduleDir : moduleDirs) {
            IFile moduleInfoFile = getFileByName(moduleDir, "module-info.java");
            if (moduleInfoFile == null) continue;

            CompilationUnit cu = parseJavaFile(moduleInfoFile);
            ModuleInfoModifier.modifyModuleInfo(cu);

            overwriteFile(moduleInfoFile, cu);
        }
    }

    private static CompilationUnit parseJavaFile(IFile file) {
        try (InputStream stream = file.getContents()) {
            return StaticJavaParser.parse(stream);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse file: " + file.getFullPath(), e);
        }
    }

    private static void overwriteFile(IFile file, CompilationUnit cu) {
        try (InputStream stream = new ByteArrayInputStream(cu.toString().getBytes(StandardCharsets.UTF_8))) {
            if (file.exists()) {
                file.setContents(stream, true, true, null);
            } else {
                file.create(stream, true, null);
            }
        } catch (CoreException | IOException | RuntimeException e) {
            throw new RuntimeException("Failed to write to file: " + file.getFullPath(), e);
        }
    }

    private static IFile getFileByName(IFolder folder, String fileName) {
        try {
            for (IResource resource : folder.members()) {
                if (resource instanceof IFile file && file.getName().contains(fileName)) {
                    return file;
                } else if (resource instanceof IFolder subFolder) {
                    IFile found = getFileByName(subFolder, fileName);
                    if (found != null) return found;
                }
            }
        } catch (CoreException e) {
            throw new RuntimeException("Error accessing folder: " + folder.getFullPath(), e);
        }
        return null;
    }
}
