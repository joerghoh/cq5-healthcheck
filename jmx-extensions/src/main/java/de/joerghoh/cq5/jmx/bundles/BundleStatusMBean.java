package de.joerghoh.cq5.jmx.bundles;

public interface BundleStatusMBean {

	/**
	 * Return the number of bundles, which are "active" state
	 */
	public int getActiveBundles();

	/**
	 * Determine hte number of bundles which are fragment bundles
	 */
	public int getFragmentBundles();

	/**
	 * Determine the number of resolved bundles
	 */
	public int getResolvedBundles();

	/**
	 * Determine teh number of installed bundles
	 */
	public int getInstalledBundles();
}
