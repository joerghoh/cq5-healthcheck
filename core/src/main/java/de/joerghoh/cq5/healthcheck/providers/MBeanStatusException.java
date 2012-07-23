package de.joerghoh.cq5.healthcheck.providers;

public class MBeanStatusException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = -1059507437247331368L;
	
	public MBeanStatusException (String message) {
		super (message);
	}

}
