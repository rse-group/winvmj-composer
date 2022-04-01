package de.ovgu.featureide.core.winvmj.core;

import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.core.resources.IFolder;

public abstract class WinVMJProduct {
	
	protected List<IFolder> modules;
	protected String productName;
	protected String splName;

	public List<IFolder> getModules() {
		return modules;
	}
	
	public List<String> getModuleNames() {
		return modules.stream().map(IFolder::getName).collect(Collectors.toList());
	}

	public String getProductName() {
		return productName;
	}
	
	public String getProductQualifiedName() {
		return splName.toLowerCase() + ".product." + productName.toLowerCase();
	}
}
