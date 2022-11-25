package de.ovgu.featureide.core.winvmj.ui.handlers;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.dialogs.ListDialog;
import org.eclipse.ui.model.WorkbenchLabelProvider;

import de.ovgu.featureide.core.IFeatureProject;
import de.ovgu.featureide.core.winvmj.routinggen.RoutingGenerator;
import de.ovgu.featureide.core.winvmj.runtime.WinVMJConsole;
import de.ovgu.featureide.fm.core.job.LongRunningMethod;
import de.ovgu.featureide.fm.core.job.LongRunningWrapper;
import de.ovgu.featureide.fm.core.job.monitor.IMonitor;
import de.ovgu.featureide.ui.handlers.base.AFeatureProjectHandler;

public class RoutingGenHandler extends AFeatureProjectHandler {

	@Override
	protected void singleAction(IFeatureProject winVmjProject) {
		final LongRunningMethod<Boolean> job = new LongRunningMethod<Boolean>() {

			@Override
			public Boolean execute(IMonitor<Boolean> workMonitor) throws Exception {
				WinVMJConsole.println("Locating mapping file...");
				IFile mappingFile = winVmjProject.getProject().getFile("FeatureMapping.xml");
				if (!mappingFile.exists()) {
					WinVMJConsole.println("Mapping file not found.");
					return false;
				}
				
				IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
				IProject[] projects = workspaceRoot.getProjects();
				List<IProject> filteredProject = new ArrayList<>();
				WinVMJConsole.println("Filtering project...");
				for (IProject project : projects) {
					if (projectIsReact(project)) {
						filteredProject.add(project);
					}
				}
				
				Display display = new Display();
				
				ListDialog dialog = createProjectSelectorDialog(filteredProject,
						"Target Project Selector",
						"Select target project to generate routes and menus files");
				if (dialog.open() == Window.OK) {
					Object[] result = dialog.getResult();
					if (result.length > 0) {
						IProject targetProject = (IProject)Platform.getAdapterManager().getAdapter(result[0], IProject.class);
						
						WinVMJConsole.println("Begin generating...");
						long start = System.currentTimeMillis();
						RoutingGenerator.generateRouting(winVmjProject, targetProject, mappingFile);
						long finish = System.currentTimeMillis();
						double elapsedTime = (finish-start)/1000.0;
						WinVMJConsole.println("Menu and Routing generated in " 
								+ String.valueOf(elapsedTime) 
								+ " seconds. Please Refresh the project "
								+ "now and find target folder.");
					} else {
						WinVMJConsole.println("No target project selected.");
					}
				} else {
					WinVMJConsole.println("Generating cancelled.");
				}
				
				dialog.close();
				display.dispose();
				return true;
			}
		};
		LongRunningWrapper.getRunner(job, "Compile JAR").schedule();
	}
	
	private static boolean projectIsReact(IProject project) {
		IFolder srcFolder = project.getFolder("src");
		return project.getFile("package.json").exists() &&
				srcFolder.exists() &&
				srcFolder.getFile("index.js").exists();
	}
	
	private static ListDialog createProjectSelectorDialog(Object input, String title, String messages) {
		ListDialog dialog = new ListDialog(null);
		dialog.setContentProvider(new ArrayContentProvider());
		dialog.setLabelProvider(new WorkbenchLabelProvider());
		dialog.setTitle(title);
		dialog.setMessage(messages);
		dialog.setInput(input);
		return dialog;
	}
}
