package de.ovgu.featureide.core.winvmj;

import de.ovgu.featureide.fm.core.FMComposerExtension;

public class WinVMJFMComposer extends FMComposerExtension {
	
	private static String ORDER_PAGE_MESSAGE = 
			"FeatureIDE projects based on WinVMJ do not need a total order\n" +
			"as a partial order can be given directly by ordering the mapping\n" +
			"on `feature_to_module.json`.";

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.ovgu.featureide.fm.core.IFMComposerExtension#getComposerName()
	 */
	@Override
	public String getOrderPageMessage() {
		return ORDER_PAGE_MESSAGE;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.ovgu.featureide.fm.core.IFMComposerExtension#hasFeaureOrder()
	 */
	@Override
	public boolean hasFeatureOrder() {
		return false;
	}

}
