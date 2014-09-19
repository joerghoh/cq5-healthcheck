package de.joerghoh.cq5.healthcheck;

/**
 * Interface for status providers.
 */
public interface StatusProvider {
	public static String DEFAULT_CATEGORY = "default";
	
	
	/**
	 * Returns the status of this provider instance.
	 * @return provider status
	 */
	public Status getStatus();
	
	/**
	 * Returns the category of this provider.
	 * @return provider category or {@link DEFAULT_CATEGORY} if no specific category is defined
	 */
	public String getCategory();
}
