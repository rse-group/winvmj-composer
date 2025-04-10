package de.ovgu.featureide.core.winvmj.compile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

import de.ovgu.featureide.core.winvmj.runtime.WinVMJConsole;

public class JavaCLI {
	
	public static void execute(String startLog, String finishLog, 
			List<String> commandStrings) throws IOException {
		ProcessBuilder pb = new ProcessBuilder(commandStrings);
		WinVMJConsole.println(startLog);
		Process proc = pb.start();
		// blocking
		BufferedReader reader = 
                new BufferedReader(new InputStreamReader(proc.getInputStream()));
		String line = null;
		while ( (line = reader.readLine()) != null) WinVMJConsole.println(line);
		reader.close();
		
		reader = new BufferedReader(new InputStreamReader(proc.getErrorStream()));
		line = null;
		while ( (line = reader.readLine()) != null) WinVMJConsole.println(line);
		reader.close();

		WinVMJConsole.println(finishLog);
	}
}
