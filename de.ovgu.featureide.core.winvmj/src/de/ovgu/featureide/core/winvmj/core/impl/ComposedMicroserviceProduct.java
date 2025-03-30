package de.ovgu.featureide.core.winvmj.core.impl;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.runtime.CoreException;
import de.ovgu.featureide.core.IFeatureProject;

public class ComposedMicroserviceProduct extends ComposedProduct {
	public ComposedMicroserviceProduct(IFeatureProject project, IFolder productModule)
			throws CoreException {
		super(project,productModule);
		
		// Add messaging module 
		IFolder messagingModule = project.getBuildFolder().getFolder("vmj.messaging");
		this.modules.add(0, messagingModule); // First module to compile	
	}
	
}
