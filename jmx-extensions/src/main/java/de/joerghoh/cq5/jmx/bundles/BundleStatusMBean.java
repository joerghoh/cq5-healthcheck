package de.joerghoh.cq5.jmx.bundles;

public interface BundleStatusMBean {

	/**
	 * Return the number of bundles, which are "active" state
	 * @return
	 */
	public int getActiveBundles();
	
	/**
	 * Determine hte number of bundles which are fragment bundles
	 * @return
	 */
	public int getFragmentBundles();
}
