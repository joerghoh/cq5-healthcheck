package de.joerghoh.cq5.healthcheck;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HealthStatus {

	private Logger log = LoggerFactory.getLogger(HealthStatus.class);
	
	private int status;
	private String reason;
	private String provider;
	
	public HealthStatus (int status, String reason, String provider) {
		
		this.status = status;
		this.reason = reason;
		this.provider = provider;
	}
	
	public int getStatus () {
		return status;
	}
	
	public String getStatusText () {
		switch (status) {
		case de.joerghoh.cq5.healthcheck.HealthStatusProvider.HS_OK: return "OK"; 
		case de.joerghoh.cq5.healthcheck.HealthStatusProvider.HS_WARN: return "WARN"; 
		case de.joerghoh.cq5.healthcheck.HealthStatusProvider.HS_ERROR: return "ERROR";
		default: log.error("Invalid status: " + status); return "ERROR";
		}
	}
	
	public String getMessage() {
		return reason;
	}
	
	public String getProvider() {
		return this.provider;
	}
	
	
}
