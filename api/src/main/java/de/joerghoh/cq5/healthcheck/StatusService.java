package de.joerghoh.cq5.healthcheck;

/**
 * Queries all registered StatusProvider services and consolidates the various
 * results into a single result.
 */
public interface StatusService {
	
	/**
	 * Returns the overall status.
	 * @return system status
	 */
	public Status getStatus();
	
	/**
	 * Returns the status for the defined categories.
	 * @param categories List of status categories
	 * @return status for the defined categories
	 */
	public Status getStatus(String[] categories);
	
	/**
	 * Returns the status for the defined categories.
	 * @param categories List of status categories
	 * @param bundleNumberThreshold Overwrites the globally configured bundle number threshold
	 * @return status for the defined categories
	 */
	public Status getStatus(String[] categories, int bundleNumberThreshold);
}
