package de.joerghoh.cq5.healthcheck;

/**
 * Queries all registered StatusProvider services and consolidates the various
 * results into a single result.
 */
public interface StatusService {
	public Status getStatus();
}
