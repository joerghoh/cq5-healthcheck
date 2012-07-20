package de.joerghoh.cq5.healthcheck;

/**
 * The HealthStatusService
 * @author joerg
 *
 * This service queries all registered HealthStatusProvider services and consolidates
 * the various results into a single result.
 */
public interface HealthStatusService {

	public OverallHealthStatus getOverallStatus();
}
