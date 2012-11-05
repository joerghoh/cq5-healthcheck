package de.joerghoh.cq5.jmx.requests;

public interface RequestInformationMBean {

	public String getMimeType();

	public long getRequestCounter();

	public long getTotalRequestDuration();
}
