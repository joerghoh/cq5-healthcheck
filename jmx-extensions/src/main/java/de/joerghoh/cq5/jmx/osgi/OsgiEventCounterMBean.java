package de.joerghoh.cq5.jmx.osgi;

public interface OsgiEventCounterMBean {

	/**
	 * Return the number of OSGI events which have been posted since startup
	 * @return
	 */
	long getTotalEventCounter();
	
	/**
	 * Returns the number of events, which applies to Sling resources (event: org/apache/sling/api/resource/Resource/*)
	 * @return
	 */
	long getResourceEventCounter();
	
	/**
	 * Returns the number of events, which apply to replication jobs (event: com/day/cq/replication/job/publish)
	 * @return
	 */
	long getReplicationEventCounter();

	
	/**
	 * Returns the number of all OSGI relevant events (org/osgiframework/*")
	 * @return
	 */
	long getOsgiEventCounter();
	
}
