package de.ovgu.featureide.core.winvmj.internal;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.apache.commons.io.FileUtils;

public class InternalResourceManager {
	
	public static void loadResourceDirectory(String resourceDirPath, String outPath) throws IOException {
		final File file = new File(InternalResourceManager.class
				.getProtectionDomain().getCodeSource()
				.getLocation().getPath());
		System.out.println(file.getAbsolutePath());
		if (file.isFile()) {
			loadJarResource(file, resourceDirPath, outPath);
		} else {
			Path srcResource = Path.of(file.getAbsolutePath(), "resources", resourceDirPath);
			FileUtils.copyDirectory(srcResource.toFile(), new File(outPath));
		}
	}
	
	public static void loadJarResource(File jarPlugin, String resourceDirPath, String outPath) throws IOException {
		final JarFile jar = new JarFile(jarPlugin);
	    final Enumeration<JarEntry> entries = jar.entries();
	    while(entries.hasMoreElements()) {
	        final String name = entries.nextElement().getName();
	        if (name.startsWith(resourceDirPath + "/")) {
	        	InputStream initScript = InternalResourceManager.class.getResourceAsStream("/" + name);
	        	String outFileName = name.replaceFirst(resourceDirPath, "");
	        	File targetFile = new File(new File(outPath), outFileName);
	        	System.out.println(targetFile.getAbsolutePath());
	        	if (outFileName.endsWith("/")) {
	        		targetFile.mkdirs();
	        	} else {
	        		targetFile.createNewFile();
		    		Files.copy(initScript, targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
	        	}
	            System.out.println("Loaded Resource: " + name + " into: " + targetFile.getAbsolutePath());
	        }
	    }
	    jar.close();
	}
	
	public static void loadResourceFile(String resourceFilePath, String outPath) throws IOException {
		final File file = new File(InternalResourceManager.class
				.getProtectionDomain().getCodeSource()
				.getLocation().getPath());
		System.out.println(file.getAbsolutePath());
		if (! file.isFile()){
			Path srcResource = Path.of(file.getAbsolutePath(), "resources", resourceFilePath);
			FileUtils.copyFile(srcResource.toFile(), new File(outPath));
		}
	}
}
