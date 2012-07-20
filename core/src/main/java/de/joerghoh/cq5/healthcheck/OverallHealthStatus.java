package de.joerghoh.cq5.healthcheck;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.joerghoh.cq5.healthcheck.HealthStatus;
import de.joerghoh.cq5.healthcheck.HealthStatusProvider;

public class OverallHealthStatus {
	
	Logger log = LoggerFactory.getLogger(OverallHealthStatus.class);
	
	private List<HealthStatus> results;
	private int status;
	
	public OverallHealthStatus (int status, List<HealthStatus> items) {
		this.status = status;
		results = items;
	}

	public String getStatus() {
		switch (status) {
			case de.joerghoh.cq5.healthcheck.HealthStatusProvider.HS_OK: return "OK"; 
			case de.joerghoh.cq5.healthcheck.HealthStatusProvider.HS_WARN: return "WARN"; 
			case de.joerghoh.cq5.healthcheck.HealthStatusProvider.HS_ERROR: return "ERROR";
			default: log.error("Invalid status: " + status); return "ERROR";
		}
	}
	
	public List<HealthStatus> getDetails() {
		return results;
	}
	
}
