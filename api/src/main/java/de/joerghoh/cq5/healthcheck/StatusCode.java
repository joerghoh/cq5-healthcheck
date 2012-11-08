package de.joerghoh.cq5.healthcheck;

public enum StatusCode {
	OK("OK"), 
	
	WARN("WARN"), 
	
	CRITICAL("CRITICAL"), 
	
	UNKNOWN("ARCHIVE");
	
	private String msg;
	
	StatusCode(String msg) {
		this.msg = msg;
	}

	@Override
	public String toString() {
		return msg;
	}
}
