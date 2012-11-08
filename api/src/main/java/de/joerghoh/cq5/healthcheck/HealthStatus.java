package de.joerghoh.cq5.healthcheck;

public class HealthStatus extends Status {
	private String provider;

	public HealthStatus(StatusCode status, String msg, String provider) {
		super(status, msg);
		this.provider = provider;
	}

	public String getProvider() {
		return this.provider;
	}
}
