package de.joerghoh.cq5.healthcheck;

/**
 * Queries all registered HealthStatusProvider services and consolidates the
 * various results into a single result.
 * 
 * @author joerg
 */
public interface HealthStatusService {
	public SystemStatus getOverallStatus();
}
