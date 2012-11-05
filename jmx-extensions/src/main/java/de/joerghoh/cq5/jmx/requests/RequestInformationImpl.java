package de.joerghoh.cq5.jmx.requests;

import java.util.concurrent.atomic.AtomicLong;

public class RequestInformationImpl implements RequestInformationMBean {

	private AtomicLong counter = new AtomicLong(0);

	private AtomicLong duration = new AtomicLong(0);

	private String mimeType;

	// MBean interface

	public String getMimeType() {
		return mimeType;
	}

	public long getRequestCounter() {
		return counter.get();
	}

	public long getTotalRequestDuration() {
		return duration.get();
	}

	// protected interface

	protected RequestInformationImpl(String mimeType) {
		this.mimeType = mimeType;
	}

	protected void update(long millis) {
		counter.incrementAndGet();
		duration.addAndGet(millis);

	}
}
