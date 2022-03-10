package de.ovgu.featureide.core.winvmj;

import org.osgi.framework.BundleContext;

import de.ovgu.featureide.fm.core.AbstractCorePlugin;

public class WinVMJCorePlugin extends AbstractCorePlugin {
	
	public static final String PLUGIN_ID = "de.ovgu.featureide.core.winvmj";
	
	private static WinVMJCorePlugin plugin;

	public WinVMJCorePlugin() {
		// TODO Auto-generated constructor stub
	}

	public String getID() {
		// TODO Auto-generated method stub
		return PLUGIN_ID;
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
	 */
	@Override
	public void stop(BundleContext context) throws Exception {
		plugin = null;
		super.stop(context);
	}
	
	public static WinVMJCorePlugin getDefault() {
		return plugin;
	}

}
