package de.ovgu.featureide.core.winvmj.core;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;

import de.ovgu.featureide.core.IFeatureProject;
import de.ovgu.featureide.core.winvmj.runtime.WinVMJConsole;

public class WinVMJProductCompiler {
	private WinVMJProduct product;
	private IFeatureProject project;
	
	public WinVMJProductCompiler(WinVMJProduct product, IFeatureProject project) {
		this.product = product;
		this.project = project;
	}
	
	public void compile() {
		
	}
	
	public void printModulePath() throws CoreException {
		for (String module: product.getModules()) {
			List<String> fileNames = transverseModuleFilePaths(project.getBuildFolder().getFolder(module));
			System.out.println(module);
			System.out.println(String.join("\n", fileNames));
			System.out.println();
		}
	}
	
	public void compileModules() throws CoreException, IOException {
		for (String module: product.getModules()) {
			compileModuleForProduct(module, product.getProductQualifiedName());
		}
	}
	
	public void compileModuleForProduct(String module, String productModule) throws CoreException, IOException {
		List<String> modulePaths = transverseModuleFilePaths(project.getBuildFolder().getFolder(module));
		IFolder compiledProductFolder = project.getProject().getFolder("src-gen").getFolder(productModule);
		IFolder binModuleFolder = project.getProject().getFolder("bin-comp").getFolder(module);
		
		List<String> compileCommand = new ArrayList<>();
		compileCommand.add("javac");
		compileCommand.add("-d");
		compileCommand.add(binModuleFolder.getLocation().toOSString());
		compileCommand.add("--module-path");
		compileCommand.add(compiledProductFolder.getLocation().toOSString());
		compileCommand.addAll(modulePaths);

		ProcessBuilder compilePb = new ProcessBuilder(compileCommand);
		WinVMJConsole.println("Compiling " + module + " module...");
		Process compileProcess = compilePb.start();
		BufferedReader reader = 
                new BufferedReader(new InputStreamReader(compileProcess.getInputStream()));
		String line = null;
		while ( (line = reader.readLine()) != null) WinVMJConsole.println(line);
		reader.close();
		WinVMJConsole.println(module + " module compiled");
		
		IFile compiledModuleFile = compiledProductFolder.getFile(module + ".jar");
		System.out.println(compiledModuleFile.getLocation().toOSString());
		
		ProcessBuilder jarPb = new ProcessBuilder("jar", "--create", "--file", 
				compiledModuleFile.getLocation().toOSString(), 
				"-C", binModuleFolder.getLocation().toOSString(), ".");
		WinVMJConsole.println("Packaging " + module + " module...");
		Process jarProcess = jarPb.start();
		reader = new BufferedReader(new InputStreamReader(jarProcess.getInputStream()));
		line = null;
		while ( (line = reader.readLine()) != null) WinVMJConsole.println(line);
		reader.close();
		WinVMJConsole.println(module + " module packaged");
	}
	
	private List<String> transverseModuleFilePaths(IFolder module) throws CoreException {
		List<String> fileNames = new ArrayList<>();
		transverseModuleFilePaths(module, fileNames);
		return fileNames;
	}
	
	private void transverseModuleFilePaths(IFolder submodule, List<String> fileNames) throws CoreException {
		for (IResource resource: submodule.members()) {
			if (resource instanceof IFolder) transverseModuleFilePaths((IFolder) resource, fileNames);
			else if (resource instanceof IFile) fileNames.add(resource.getLocation().toOSString());
		}
	}

}
