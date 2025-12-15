package id.ac.ui.cs.prices.winvmj.composer.ui.handlers;

import de.ovgu.featureide.core.IFeatureProject;
import de.ovgu.featureide.fm.core.job.LongRunningMethod;
import de.ovgu.featureide.fm.core.job.LongRunningWrapper;
import de.ovgu.featureide.fm.core.job.monitor.IMonitor;
import de.ovgu.featureide.ui.handlers.base.AFeatureProjectHandler;
import id.ac.ui.cs.prices.winvmj.composer.WinVMJComposer;
import id.ac.ui.cs.prices.winvmj.composer.runtime.WinVMJConsole;


public class MicroservicesComposeHandler extends AFeatureProjectHandler {

	@Override
	protected void singleAction(IFeatureProject project) {
		WinVMJConsole.showConsole();
		final LongRunningMethod<Boolean> job = new LongRunningMethod<Boolean>() {

			@Override
			public Boolean execute(IMonitor<Boolean> workMonitor) throws Exception {
				WinVMJConsole.println("Begin compose microservices product...");
				
		        ((WinVMJComposer) project.getComposer()).performFullBuildMicroservices();
				
				return true;
			}
		};
		LongRunningWrapper.getRunner(job, "Compose Microservices").schedule();
	}
	
}
