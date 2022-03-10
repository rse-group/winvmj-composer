package de.ovgu.featureide.core.winvmj.runtime;

import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.console.MessageConsoleStream;

public class WinVMJConsole {
	final static String CONSOLE_NAME = "WinVMJ Console";
	
	private static MessageConsoleStream consoleStream = initConsole();
	
	private WinVMJConsole() {};
	
	private static MessageConsoleStream initConsole() {
		MessageConsole console = new MessageConsole(CONSOLE_NAME, null);
		console.activate();
		ConsolePlugin.getDefault().getConsoleManager().addConsoles(new IConsole[]{ (IConsole) console });
		return console.newMessageStream();
	}
	
	public static void println() {
		consoleStream.println();
	}
	
	public static void println(String message) {
		consoleStream.println(message);
	}
	
	public static void print(String message) {
		consoleStream.print(message);
	}
}
