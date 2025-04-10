package de.ovgu.featureide.core.winvmj.runtime;

import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleConstants;
import org.eclipse.ui.console.IConsoleView;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.console.MessageConsoleStream;

public class WinVMJConsole {
	final static String CONSOLE_NAME = "WinVMJ Console";

	private static MessageConsole consoleInstance;
	private static MessageConsoleStream consoleStream = initConsole();
	
	private WinVMJConsole() {};
	
	private static MessageConsoleStream initConsole() {
		MessageConsole console = new MessageConsole(CONSOLE_NAME, null);
		consoleInstance = console;
		console.activate();
		ConsolePlugin.getDefault().getConsoleManager().addConsoles(new IConsole[]{ (IConsole) console });
		return console.newMessageStream();
	}
	
	public static void showConsole() {
		IWorkbench wb = PlatformUI.getWorkbench();
		IWorkbenchWindow win = wb.getActiveWorkbenchWindow();
		IWorkbenchPage page = win.getActivePage();
		try {
			IConsoleView view = (IConsoleView) page.showView(IConsoleConstants.ID_CONSOLE_VIEW);
			view.display(consoleInstance);
		} catch (PartInitException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
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
