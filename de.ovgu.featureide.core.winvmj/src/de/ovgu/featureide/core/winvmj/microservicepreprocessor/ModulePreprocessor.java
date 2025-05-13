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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class ModulePreprocessor {

    public static Map<String, Set<String>> modifyServiceImplClass(Set<IFolder> moduleDirs) {
        Set<String> domainInterfacesFqn = ModelLayerExtractor.extractModelInterfacesFqn(moduleDirs);
        Set<String> domainInterfaces = domainInterfacesFqn.stream()
                .map(fqn -> fqn.substring(fqn.lastIndexOf('.') + 1))
                .collect(Collectors.toSet());

        PublishMessageCallAdder publishMessageCallAdder = new PublishMessageCallAdder(domainInterfaces);
        
        Map<String, Set<String>> moduleRoutingKeyMap = new HashMap<String, Set<String>>();
        for (IFolder moduleDir : moduleDirs) {
        	Set<String> routingKeyValues = new HashSet<String>();
            List<IFile> serviceImplFiles = getFilesByName(moduleDir, "ServiceImpl.java");
            for (IFile serviceImplFile : serviceImplFiles ) {
            	if (serviceImplFile == null) continue;

                CompilationUnit cu = JavaParserUtil.parse(serviceImplFile);
                
                String messagingModule = "vmj.messaging";
                List<String> requiredImports = List.of(
                        "java.util.*",
                        messagingModule + ".StateTransferMessage",
                        messagingModule + ".Property",
                        messagingModule + ".rabbitmq.RabbitMQManager"
                );
                String routingKeyValue =  QueueBindingTrigger.addRoutingKeyAttribute(cu);
                routingKeyValues.add(routingKeyValue);
                addImportStatement(cu, requiredImports);
                publishMessageCallAdder.addPublishMessageCall(cu);

                overwriteFile(serviceImplFile, cu);
            }
            moduleRoutingKeyMap.put(moduleDir.getName(), routingKeyValues);
            
        }
        return moduleRoutingKeyMap;
    }

    public static void modifyProductModule(Set<IFolder> moduleDirs, Set<String> routingKeyValues,
    		IFolder productModule, String productName) {
    	try {
    		productModule.refreshLocal(IResource.DEPTH_INFINITE, null);
    		IFolder mainClassDir = getMainClassDir(productModule);
    		copyMessageConsumer(mainClassDir.getLocation().toOSString());
    		mainClassDir.refreshLocal(IResource.DEPTH_INFINITE, null);
    		
    		IFile messageConsumerFile = getFileByName(mainClassDir, "MessageConsumerImpl.java");
            if (messageConsumerFile == null) return;
            
            // Modify MessageConsumer
            CompilationUnit messageConsumerCu = JavaParserUtil.parse(messageConsumerFile);
            
            List<String> domainModelInterfacesFqn = new ArrayList<String>(ModelLayerExtractor.extractModelInterfacesFqn(moduleDirs));
            addImportStatement(messageConsumerCu, domainModelInterfacesFqn);
            
            Set<String> domainImplementationsFqnSet = ModelLayerExtractor.extractModelImplementationsFqn(moduleDirs);
            ConcreteClassCaster.modifySetAttributesMethod(messageConsumerCu, domainImplementationsFqnSet);
            
            QueueBindingTrigger.addQueueBindingCall(messageConsumerCu, routingKeyValues);
            
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

    public static void modifyModuleInfo(IFolder moduleDir, String productModule) {
    	IFile moduleInfoFile = getFileByName(moduleDir, "module-info.java");
        if (moduleInfoFile == null) return;

        CompilationUnit cu = JavaParserUtil.parse(moduleInfoFile);
        ModuleInfoModifier.modifyModuleInfo(cu, productModule);

        overwriteFile(moduleInfoFile, cu);
    }
    
    public static void registerRoutingToApiGateway(Map<String, Set<IFolder>> serviceFeatureModuleMap, IFile apiGatewayFile) {
    	
    	Map<String, Set<String>> serviceToEndpointsMap = new HashMap<String, Set<String>>();
    	for (Map.Entry<String, Set<IFolder>> entry : serviceFeatureModuleMap.entrySet()) {
			String serviceName = entry.getKey();
			Set<IFolder> selectedFeatureModules = entry.getValue();
			
			Set<String> exposedServiceEndpoints = ResourceLayerExtractor.extractEndpoints(selectedFeatureModules);
			serviceToEndpointsMap.put(serviceName, exposedServiceEndpoints);
    	}
    	
    	CompilationUnit cu = JavaParserUtil.parse(apiGatewayFile);
    	RoutingMapAdder.addRoutingMap(cu, serviceToEndpointsMap);

        overwriteFile(apiGatewayFile, cu);
    }
    
    
    public static void deleteResourceLayer(IFolder moduleDir) {
        try {
            for (IResource resource : moduleDir.members()) {
                if (resource instanceof IFolder folder) {
                    if (folder.getName().equals("resource")) {
                        folder.delete(true, null);
                    } else {
                        deleteResourceLayer(folder); 
                    }
                } else if (resource instanceof IFile file) {
                    String name = file.getName();
                    if (name.endsWith("ResourceFactory.java")) {
                        file.delete(true, null);
                    }
                }
            }
        } catch (CoreException e) {
            e.printStackTrace();
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
    
    private static List<IFile> getFilesByName(IFolder folder, String fileName) {
        List<IFile> matchedFiles = new ArrayList<>();

        try {
            for (IResource resource : folder.members()) {
                if (resource instanceof IFile file && file.getName().contains(fileName)) {
                    matchedFiles.add(file);
                } else if (resource instanceof IFolder subFolder) {
                    matchedFiles.addAll(getFilesByName(subFolder, fileName));
                }
            }
        } catch (CoreException e) {
            throw new RuntimeException("Error accessing folder: " + folder.getFullPath(), e);
        }

        return matchedFiles;
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
