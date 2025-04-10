package de.ovgu.featureide.core.winvmj.templates;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;

import de.ovgu.featureide.core.winvmj.templates.impl.ModuleInfoRenderer;
import freemarker.cache.StringTemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.Version;

public abstract class ReactTemplateRenderer {
	
	protected Configuration configuration;
	protected Map<String, Object> featureMap;
	protected String[] selectedFeature;
	
	public ReactTemplateRenderer(String[] selectedFeature, Map<String, Object> featureMap) {
		this.selectedFeature = selectedFeature;
		this.featureMap = featureMap;
		configuration = new Configuration(new Version(2, 3, 31));
	}
	
	public void render(IProject targetProject) {
		Map<String, Object> dataModel = extractDataModel();
		Template template = loadTemplate(loadTemplateFilename());
		IFile outputFile = getOutputFile(targetProject);
		write(dataModel, template, outputFile);
	}
	
	protected abstract Map<String, Object> extractDataModel();
	
	protected abstract String loadTemplateFilename();
	
	protected abstract IFile getOutputFile(IProject targetProject);
	
	private Template loadTemplate(String templateName) {
		String fullTemplateName = templateName + ".ftl";
		InputStream moduleStream = ModuleInfoRenderer.class.getResourceAsStream("/templates/" + fullTemplateName);
		try {
			String templateString = new String(moduleStream.readAllBytes(), StandardCharsets.UTF_8);
			moduleStream.close();
			StringTemplateLoader stringloader = new StringTemplateLoader();
			stringloader.putTemplate(fullTemplateName, templateString);
			configuration.setTemplateLoader(stringloader);
			return configuration.getTemplate(fullTemplateName);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	
	private void write(Map<String, Object> dataModel, Template template, IFile outputFile) {
		Writer writer = new StringWriter();
		try {
			template.process(dataModel, writer);
			writer.close();
		} catch (TemplateException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		InputStream contentStream = new ByteArrayInputStream(writer.toString().getBytes());
		try {
			if (outputFile.exists()) outputFile.setContents(contentStream, false, false, null);
			else outputFile.create(contentStream, false, null);
		} catch (CoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
