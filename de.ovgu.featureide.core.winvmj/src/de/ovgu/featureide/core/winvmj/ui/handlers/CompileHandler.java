package de.ovgu.featureide.core.winvmj.ui.handlers;

import de.ovgu.featureide.core.IFeatureProject;
import de.ovgu.featureide.core.winvmj.compile.SourceCompiler;
import de.ovgu.featureide.core.winvmj.runtime.WinVMJConsole;
import de.ovgu.featureide.fm.core.job.LongRunningMethod;
import de.ovgu.featureide.fm.core.job.LongRunningWrapper;
import de.ovgu.featureide.fm.core.job.monitor.IMonitor;
import de.ovgu.featureide.ui.handlers.base.AFeatureProjectHandler;

public class CompileHandler extends AFeatureProjectHandler {

	@Override
	protected void singleAction(IFeatureProject project) {
		WinVMJConsole.showConsole();
		final LongRunningMethod<Boolean> job = new LongRunningMethod<Boolean>() {

			@Override
			public Boolean execute(IMonitor<Boolean> workMonitor) throws Exception {
				WinVMJConsole.println("Begin compiling...");
				long start = System.currentTimeMillis();
				SourceCompiler.compileSource(project);
				long finish = System.currentTimeMillis();
				double elapsedTime = (finish-start)/1000.0;
				WinVMJConsole.println("Compile process completed in " 
				+ String.valueOf(elapsedTime) 
				+ " seconds. Please Refresh the project "
				+ "now and find your compiled product on src-gen.");
				return true;
			}
		};
		LongRunningWrapper.getRunner(job, "Compile JAR").schedule();
	}
}
