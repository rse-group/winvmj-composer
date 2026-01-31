package id.ac.ui.cs.prices.winvmj.composer.compile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

import id.ac.ui.cs.prices.winvmj.composer.runtime.WinVMJConsole;

public class JavaCLI {
	
	public static void execute(String startLog, String finishLog, 
			List<String> commandStrings) throws IOException {
		ProcessBuilder pb = new ProcessBuilder(commandStrings);
		pb.redirectErrorStream(true); // Merge stderr into stdout to prevent deadlock
		WinVMJConsole.println(startLog);
		Process proc = pb.start();
		
		BufferedReader reader = 
                new BufferedReader(new InputStreamReader(proc.getInputStream()));
		String line = null;
		while ( (line = reader.readLine()) != null) {
			WinVMJConsole.println(line);
		}
		reader.close();
		
		try {
			int exitCode = proc.waitFor();
			if (exitCode != 0) {
				WinVMJConsole.println("WARNING: Command exited with code " + exitCode);
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}

		WinVMJConsole.println(finishLog);
	}
}
