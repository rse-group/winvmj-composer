package de.ovgu.featureide.core.winvmj.ui.handlers;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import de.ovgu.featureide.core.IFeatureProject;
import de.ovgu.featureide.core.winvmj.WinVMJComposer;

import de.ovgu.featureide.core.winvmj.runtime.WinVMJConsole;
import de.ovgu.featureide.fm.core.base.IFeature;
import de.ovgu.featureide.fm.core.job.LongRunningMethod;
import de.ovgu.featureide.fm.core.job.LongRunningWrapper;
import de.ovgu.featureide.fm.core.job.monitor.IMonitor;
import de.ovgu.featureide.ui.handlers.base.AFeatureProjectHandler;


public class MicroservicesComposeHandler extends AFeatureProjectHandler {

	@Override
	protected void singleAction(IFeatureProject project) {
		WinVMJConsole.showConsole();
		final LongRunningMethod<Boolean> job = new LongRunningMethod<Boolean>() {

			@Override
			public Boolean execute(IMonitor<Boolean> workMonitor) throws Exception {
				WinVMJConsole.println("Begin compose microservices product...");
				
				IFile servicesDefFile = project.getProject().getFile("services-def.json");

				Map<String, List<IFeature>> serviceDefinition = readServiceDefinition(project, servicesDefFile);
		        
		        ((WinVMJComposer) project.getComposer()).performFullBuildMicroservices(serviceDefinition);
				
				return true;
			}
		};
		LongRunningWrapper.getRunner(job, "Compose Microservices").schedule();
	}
	
	static Map<String, List<IFeature>> readServiceDefinition(IFeatureProject project, IFile file) {
        Map<String, List<IFeature>> serviceDefinition = new HashMap<>();

        try (InputStream inputStream = file.getContents();
             InputStreamReader reader = new InputStreamReader(inputStream);
             BufferedReader bufferedReader = new BufferedReader(reader)) {
        
            JsonObject jsonObject = JsonParser.parseReader(bufferedReader).getAsJsonObject();
            JsonArray servicesArray = jsonObject.getAsJsonArray("services");

            for (JsonElement serviceElement : servicesArray) {
                JsonObject serviceObject = serviceElement.getAsJsonObject();

                String productName = serviceObject.get("productName").getAsString();

                JsonArray featuresArray = serviceObject.getAsJsonArray("features");
                List<IFeature> featuresList = new ArrayList<>();

                for (JsonElement featureElement : featuresArray) {
                	String featureString = featureElement.getAsString();
                    IFeature feature = project.getFeatureModel().getFeature(featureString);

                    featuresList.add(feature);
                }

                serviceDefinition.put(productName, featuresList);
            }
        } catch (CoreException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return serviceDefinition;
    }
}
