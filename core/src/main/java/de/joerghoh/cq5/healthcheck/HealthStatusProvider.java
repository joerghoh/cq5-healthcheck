package de.joerghoh.cq5.healthcheck;

public interface HealthStatusProvider {

	
	public static final int HS_OK = 0;
	
	
	public static final int HS_WARN = 1;
	
	
	public static final int HS_ERROR = 2;
	
	
	public HealthStatus getHealthStatus();
	
	
	
}
