package de.ovgu.featureide.core.winvmj.ui.handlers;

import org.eclipse.core.resources.IFolder;

import de.ovgu.featureide.core.IFeatureProject;
import de.ovgu.featureide.core.winvmj.compile.SourceCompiler;
import de.ovgu.featureide.core.winvmj.runtime.WinVMJConsole;
import de.ovgu.featureide.fm.core.job.LongRunningMethod;
import de.ovgu.featureide.fm.core.job.LongRunningWrapper;
import de.ovgu.featureide.fm.core.job.monitor.IMonitor;
import de.ovgu.featureide.ui.handlers.base.AFeatureProjectHandler;
import de.ovgu.featureide.fm.ui.handlers.base.AFolderHandler;
import de.ovgu.featureide.core.CorePlugin;

public class CompileModuleHandler extends AFolderHandler {

	@Override
	protected void singleAction(IFolder moduleFolder) {
		WinVMJConsole.showConsole();
		final LongRunningMethod<Boolean> job = new LongRunningMethod<Boolean>() {

			@Override
			public Boolean execute(IMonitor<Boolean> workMonitor) throws Exception {
				WinVMJConsole.println("Begin compiling modules...");
				long start = System.currentTimeMillis();
				
				final IFeatureProject project = CorePlugin.getFeatureProject(moduleFolder);
				
				SourceCompiler.compileModuleSource(moduleFolder, project);
				long finish = System.currentTimeMillis();
				double elapsedTime = (finish-start)/1000.0;
				WinVMJConsole.println("Compile modules process completed in " 
				+ String.valueOf(elapsedTime) 
				+ " seconds. Please Refresh the project "
				+ "now and find your compiled product on internal-modules");
				return true;
			}
		};
		LongRunningWrapper.getRunner(job, "Compile JAR").schedule();
	}
}
