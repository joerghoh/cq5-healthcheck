package de.joerghoh.cq5.healthcheck;

/**
 * Status value definitions are taken from
 * http://nagiosplug.sourceforge.net/developer-guidelines.html#AEN76 and are the
 * values which are defined by the popular Nagios monitoring system for the
 * communication with its plugins.
 */
public interface HealthStatusProvider {

	public static final int OK = 0;
	public static final int WARN = 1;
	public static final int CRITICAL = 2;
	public static final int UNKNOWN = 3;

	public HealthStatus getHealthStatus();
}
