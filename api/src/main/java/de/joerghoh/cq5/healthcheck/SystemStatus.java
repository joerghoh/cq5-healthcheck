package de.joerghoh.cq5.healthcheck;

import java.util.List;

public class SystemStatus extends Status {

	private List<HealthStatus> items;

	public SystemStatus(StatusCode status, String msg,
			List<HealthStatus> items) {
		super(status, msg);
		this.items = items;
	}

	public List<HealthStatus> getDetails() {
		return items;
	}
}
