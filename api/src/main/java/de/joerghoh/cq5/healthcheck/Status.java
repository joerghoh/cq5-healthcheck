package de.joerghoh.cq5.healthcheck;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class Status {
	private Logger log = LoggerFactory.getLogger(Status.class);

	private StatusCode status;
	private String msg;

	public Status(StatusCode status, String reason) {
		this.status = status;
		this.msg = reason;
	}

	public StatusCode getStatus() {
		return status;
	}

	public String getStatusText() {
		if (status == StatusCode.UNKNOWN) {
			log.error("Invalid status: " + status);
		}
		return status.toString();
	}

	public String getMessage() {
		return msg;
	}
}
