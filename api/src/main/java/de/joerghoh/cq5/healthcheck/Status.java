package de.joerghoh.cq5.healthcheck;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Status {
	private Logger log = LoggerFactory.getLogger(Status.class);

	private StatusCode status;
	private List<Status> details;
	private String provider;
	private String msg;

	public Status(StatusCode status, String reason) {
		this.status = status;
		this.msg = reason;
	}

	public Status(StatusCode status, String reason, String provider) {
		this.provider = provider;
		this.status = status;
		this.msg = reason;
	}

	public Status(StatusCode status, String reason, List<Status> details) {
		this.details = details;
		this.status = status;
		this.msg = reason;
	}

	public Status(StatusCode status, String reason, String provider,
			List<Status> details) {
		this.provider = provider;
		this.details = details;
		this.status = status;
		this.msg = reason;
	}

	public StatusCode getStatus() {
		return status;
	}

	public String getStatusText() {
		return status.toString();
	}

	public String getMessage() {
		return msg;
	}

	public String getProvider() {
		return this.provider;
	}

	public List<Status> getDetails() {
		return details;
	}
}
