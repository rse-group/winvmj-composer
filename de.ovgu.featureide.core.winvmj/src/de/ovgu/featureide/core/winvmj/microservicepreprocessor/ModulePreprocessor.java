package de.ovgu.featureide.core.winvmj.microservicepreprocessor;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;

import de.ovgu.featureide.core.winvmj.internal.InternalResourceManager;
import de.ovgu.featureide.core.winvmj.microservicepreprocessor.publish.PublishMessageCallAdder;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.CoreException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
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

            CompilationUnit cu = JavaParserUtil.parse(serviceImplFile);
            
            String messagingModule = "vmj.messaging";
            List<String> requiredImports = List.of(
                    "java.util.*",
                    messagingModule + ".StateTransferMessage",
                    messagingModule + ".Property",
                    messagingModule + ".rabbitmq.RabbitMQManager"
            );
            
            addImportStatement(cu, requiredImports);
            QueueBindingTrigger.addQueueBindingCall(cu);
            publishMessageCallAdder.addPublishMessageCall(cu);

            overwriteFile(serviceImplFile, cu);
        }
    }

    public static void modifyProductModule(Set<IFolder> moduleDirs, IFolder productModule, String productName) {
    	try {
    		productModule.refreshLocal(IResource.DEPTH_INFINITE, null);
    		IFolder mainClassDir = getMainClassDir(productModule);
    		copyMessageConsumer(mainClassDir.getLocation().toOSString());
    		mainClassDir.refreshLocal(IResource.DEPTH_INFINITE, null);
    		
    		IFile messageConsumerFile = getFileByName(mainClassDir, "MessageConsumerImpl.java");
            if (messageConsumerFile == null) return;
            
            // Modify MessageConsumer
            CompilationUnit messageConsumerCu = JavaParserUtil.parse(messageConsumerFile);
            
            ModelLayerExtractor.addModelInterfaceImportStatement(moduleDirs, messageConsumerCu);
            ModelFactoryExtractor.initializeObjectFactory(moduleDirs, messageConsumerCu);
            RepositoryExtractor.initializeRepositoryMap(moduleDirs, messageConsumerCu);
            
            // Modify MainClass
            IFile productFile = getFileByName(mainClassDir, productName);
            if (productFile == null) return;
            CompilationUnit productCu  = JavaParserUtil.parse(productFile);
            String messagingModule = "vmj.messaging";
            List<String> requiredImports = List.of(
                    messagingModule + ".rabbitmq.RabbitMQManager"
            );
            addImportStatement(productCu, requiredImports);
            MessageConsumerRegister.addMessageConsumerRegistration(productCu);
            ClassMover.moveClass(messageConsumerCu, productCu);
            
            overwriteFile(productFile, productCu);
            
            messageConsumerFile.delete(true, null);
            
		} catch (CoreException e) {
			e.printStackTrace();
		}
        
    }

    public static void modifyModuleInfo(Set<IFolder> moduleDirs) {
        for (IFolder moduleDir : moduleDirs) {
            IFile moduleInfoFile = getFileByName(moduleDir, "module-info.java");
            if (moduleInfoFile == null) continue;

            CompilationUnit cu = JavaParserUtil.parse(moduleInfoFile);
            ModuleInfoModifier.modifyModuleInfo(cu);

            overwriteFile(moduleInfoFile, cu);
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
    
    private static void addImportStatement(CompilationUnit cu, List<String> requiredImports) {
        requiredImports.forEach(importStr -> {
            boolean isAsterisk = importStr.endsWith(".*");
            String importName = isAsterisk ? importStr.substring(0, importStr.length() - 2) : importStr;

            ImportDeclaration importDecl = new ImportDeclaration(importName, false, isAsterisk);

            boolean alreadyExists = cu.getImports().stream().anyMatch(existing ->
                existing.getNameAsString().equals(importName) &&
                existing.isAsterisk() == isAsterisk
            );

            if (!alreadyExists) {
                cu.addImport(importDecl);
            }
        });
    }
    
    private static IFolder getMainClassDir(IFolder productModuleDir) throws CoreException {
        String[] segments = productModuleDir.getName().split("\\."); // <spl name>.product.<product name>
        
        if (segments.length < 3) {
            throw new IllegalArgumentException("Invalid product module folder name: " + productModuleDir.getName());
        }
        
        String splName = segments[0];
        String productName = segments[segments.length - 1];
        
        IFolder splDir = productModuleDir.getFolder(splName);
        IFolder productDir = splDir.getFolder("product");
        IFolder productNameDir = productDir.getFolder(productName);

        if (!productNameDir.exists()) {
            throw new IllegalStateException("Product name folder not found: " + productNameDir.getFullPath());
        }

        return productNameDir;
    }
    
    private static void copyMessageConsumer(String mainClassDirPath) {
    	try {
    		String messageConsumerFileName = "/MessageConsumerImpl.java";
			InternalResourceManager.loadResourceFile("microservice-preprocessor"+messageConsumerFileName, mainClassDirPath+messageConsumerFileName);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
}
