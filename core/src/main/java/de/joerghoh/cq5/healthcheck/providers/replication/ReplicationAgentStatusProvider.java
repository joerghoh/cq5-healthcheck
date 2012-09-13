package de.joerghoh.cq5.healthcheck.providers.replication;

import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.joerghoh.cq5.healthcheck.HealthStatus;
import de.joerghoh.cq5.healthcheck.HealthStatusProvider;
import de.joerghoh.cq5.healthcheck.providers.MBeanStatusException;
import de.joerghoh.cq5.healthcheck.providers.replication.ReplicationAgentStatusUtil;

/**
 * Checks the state of a replication agent MBean
 * @author joerg
 * 
 * TODO: * read agent specific limits via OSGI
 *       * implement dynamic behaviour when replication agents are created/deleted 
 *
 */
public class ReplicationAgentStatusProvider implements HealthStatusProvider {

	Logger log = LoggerFactory.getLogger(ReplicationAgentStatusProvider.class);

	private ReplicationAgentStatusOptions options;
	
	private int queueWarn;
	private int queueError;
	

	
	protected ReplicationAgentStatusProvider (ReplicationAgentStatusOptions options) {
		this.options = options;
	}
	
	/**
	 * Return the healthstatus of the agent
	 */
	public HealthStatus getHealthStatus() {
		
		queueWarn = Integer.parseInt ((String) options.getProperties().get(ReplicationAgentStatusUtil.QUEUE_WARN_LENGTH));
		queueError = Integer.parseInt ((String) options.getProperties().get(ReplicationAgentStatusUtil.QUEUE_ERROR_LENGTH));
		String message = "okok";
		String providerName = this.getClass().getName();
		int status = OK;
		try {
			providerName = "Replication Agent " + getAgentName();
			long len = getQueueLength();
			if (len > queueWarn) {
				status = WARN;
			}
			if (len > queueError) {
				status = CRITICAL;
			}
		} catch (MBeanStatusException e) {
			log.warn ("Cannot determine correct replication agent state: ",e);
			status = WARN;
		}
		log.debug ("Checking "+ options.getObjectName().toString() + "queueError="+queueError+",queueWarn="+queueWarn);
		return new HealthStatus (status,message,providerName);
	}
	
	@org.apache.felix.scr.annotations.Activate
	protected void Activate (ComponentContext ctx) {
		log.info ("Activating "+ options.getObjectName().toString() + "queueError="+queueError+",queueWarn="+queueWarn);
	}
	
	@org.apache.felix.scr.annotations.Deactivate
	protected void Deactivate (ComponentContext ctx) {
		log.info ("Dectivating " + options.getObjectName().toString());
	}

	// private helper methods
	
	private long getQueueLength() throws MBeanStatusException {
		Object r = getValue ("QueueNumEntries");
		return (Long) r;
	}
	
	
	private String getAgentName () throws MBeanStatusException {
		Object r = getValue ("Id");
		return (String) r;
	}
	
	
	private Object getValue (String property) throws MBeanStatusException {
       Object result = null;
		try {
			result =  options.getMbeanServer().getAttribute (options.getObjectName(), property);
		} catch (Exception e) {
			log.error("Cannot read property %s from MBean",property,e);
			throw new MBeanStatusException(e.getMessage());
		}
		return result;
	}
       

	
	
}
