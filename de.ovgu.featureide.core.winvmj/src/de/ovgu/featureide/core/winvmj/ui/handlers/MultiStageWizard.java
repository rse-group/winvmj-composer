package de.ovgu.featureide.core.winvmj.ui.handlers;

import de.ovgu.featureide.core.IFeatureProject;
import de.ovgu.featureide.core.winvmj.runtime.WinVMJConsole;
import de.ovgu.featureide.core.winvmj.templates.impl.MultiStageConfiguration;
import de.ovgu.featureide.fm.core.job.LongRunningMethod;
import de.ovgu.featureide.fm.core.job.LongRunningWrapper;
import de.ovgu.featureide.fm.core.job.monitor.IMonitor;
import de.ovgu.featureide.ui.handlers.base.AFeatureProjectHandler;

public class MultiStageWizard extends AFeatureProjectHandler {
	
	@Override
	protected void singleAction(IFeatureProject project) {
		WinVMJConsole.showConsole();
		final LongRunningMethod<Boolean> job = new LongRunningMethod<Boolean>() {

			@Override
			public Boolean execute(IMonitor<Boolean> workMonitor) throws Exception {
				WinVMJConsole.println("Entering Multi Level...");
				MultiStageConfiguration.multiStage(project);
				WinVMJConsole.println("Done.....");
				return true;
			}
		};
		LongRunningWrapper.getRunner(job, "Compile JAR").schedule();
	}
}
