package de.ovgu.featureide.core.winvmj.ui.handlers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.internal.resources.Project;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.ListDialog;
import org.eclipse.ui.model.BaseWorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;

import de.ovgu.featureide.core.IFeatureProject;
import de.ovgu.featureide.core.winvmj.compile.SourceCompiler;
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
				IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
				IProject[] projects = workspaceRoot.getProjects();
				List<IProject> filteredProject = new ArrayList<>();
				WinVMJConsole.println("Filtering project");
				for (IProject project : projects) {
					IFolder srcFolder = project.getFolder("src");
					if (project.getFile("package.json").exists() &&
							project.getFile("package-lock.json").exists() &&
							srcFolder.exists() &&
							srcFolder.getFile("index.js").exists()) {
						filteredProject.add(project);
					}
				}
				
				Display display = new Display();
				Shell shell = new Shell(display);
				
				ListDialog dialog = new ListDialog(shell);
				dialog.setContentProvider(new ArrayContentProvider());
				dialog.setLabelProvider(new WorkbenchLabelProvider());
				dialog.setTitle("Target Project Selector");
				dialog.setMessage("Select target project to generate routes and menus files");
				dialog.setInput(filteredProject);
				if (dialog.open() == Window.OK) {
					Object[] result = dialog.getResult();
					if (result.length > 0) {
						IProject targetProject = (IProject)Platform.getAdapterManager().getAdapter(result[0], IProject.class);
						
						WinVMJConsole.println("Begin generating...");
						long start = System.currentTimeMillis();
						RoutingGenerator.generateRouting(winVmjProject, targetProject);
						long finish = System.currentTimeMillis();
						double elapsedTime = (finish-start)/1000.0;
						WinVMJConsole.println("Menu and Routing generated " 
								+ String.valueOf(elapsedTime) 
								+ " seconds. Please Refresh the project "
								+ "now and find target folder.");
					} else {
						WinVMJConsole.println("No target project selected");
					}
				}
				dialog.close();
				display.dispose();
				return true;
			}
		};
		LongRunningWrapper.getRunner(job, "Compile JAR").schedule();
	}
}
