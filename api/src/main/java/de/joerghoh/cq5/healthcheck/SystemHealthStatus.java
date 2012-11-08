package de.joerghoh.cq5.healthcheck;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SystemHealthStatus {
	
	private Logger log = LoggerFactory.getLogger(SystemHealthStatus.class);
	
	private List<HealthStatus> results;
	private int status;
	private String monitoringMessage;
	
	public SystemHealthStatus (int status, List<HealthStatus> items, String message) {
		this.status = status;
		results = items;
		monitoringMessage = message;
	}

	public String getStatus() {
		switch (status) {
			case HealthStatusProvider.OK: return "OK"; 
			case HealthStatusProvider.WARN: return "WARN"; 
			case HealthStatusProvider.CRITICAL: return "CRITICAL";
			default: log.error("Invalid status: " + status); return "UNKNOWN";
		}
	}
	
	public List<HealthStatus> getDetails() {
		return results;
	}
	
	public String getMonitoringMessage () {
		return monitoringMessage;
	}
}
