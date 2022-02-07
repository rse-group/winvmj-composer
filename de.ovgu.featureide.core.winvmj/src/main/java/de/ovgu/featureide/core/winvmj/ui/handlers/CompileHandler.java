package de.ovgu.featureide.core.winvmj.ui.handlers;

import java.io.InputStreamReader;
import java.io.Reader;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import de.ovgu.featureide.core.IFeatureProject;
import de.ovgu.featureide.core.winvmj.WinVMJComposer;
import de.ovgu.featureide.core.winvmj.compile.SourceCompiler;
import de.ovgu.featureide.core.winvmj.runtime.WinVMJConsole;
import de.ovgu.featureide.fm.core.job.LongRunningMethod;
import de.ovgu.featureide.fm.core.job.LongRunningWrapper;
import de.ovgu.featureide.fm.core.job.monitor.IMonitor;
import de.ovgu.featureide.ui.handlers.base.AFeatureProjectHandler;

public class CompileHandler extends AFeatureProjectHandler {

	@Override
	protected void singleAction(IFeatureProject project) {
		// TODO Auto-generated method stub
		final LongRunningMethod<Boolean> job = new LongRunningMethod<Boolean>() {

			@Override
			public Boolean execute(IMonitor<Boolean> workMonitor) throws Exception {
				WinVMJConsole.println("Begin compiling...");
				try {
					getproductModule(project);
					getFeatureModules(project);
					SourceCompiler.compileSource(project);
				} catch (CoreException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				WinVMJConsole.println("Compile process completed. Please Refresh the project now.");
				return true;
			}
		};
		LongRunningWrapper.getRunner(job, "Compile JAR").schedule();
	}
	
	private void getproductModule(IFeatureProject project) throws CoreException {
		IResource productModule = Stream
				.of(project.getBuildFolder().members())
				.filter(module -> module.getName().contains(".product."))
				.findFirst().get();
		WinVMJConsole.println(productModule.getName());
	}
	
	private void getFeatureModules(IFeatureProject project) throws CoreException {
		Reader mapReader = null;
		try {
			mapReader = new InputStreamReader(project.getProject()
					.getFile(WinVMJComposer.FEATURE_MODULE_MAPPER_FILENAME)
					.getContents());
		} catch (CoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		Gson gson = new Gson();
		Map<String, String> mappings = gson.fromJson(mapReader, 
				new TypeToken<LinkedHashMap<String, String>>() {}.getType());
		
		List<String> sourceModules = Stream
				.of(project.getBuildFolder().members())
				.filter(module -> !module.getName().contains(".product."))
				.map(module -> module.getName())
				.collect(Collectors.toList());
		
		List<String> orderedSourceModules = mappings.keySet().stream()
				.filter(module -> sourceModules.contains(module))
				.collect(Collectors.toList());
		
		orderedSourceModules.forEach(WinVMJConsole::println);
	}
}
