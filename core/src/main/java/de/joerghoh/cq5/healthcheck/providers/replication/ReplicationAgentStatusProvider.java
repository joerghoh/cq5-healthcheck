package de.joerghoh.cq5.healthcheck.providers.replication;

import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.ReflectionException;

import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.joerghoh.cq5.healthcheck.HealthStatus;
import de.joerghoh.cq5.healthcheck.HealthStatusProvider;
import de.joerghoh.cq5.healthcheck.providers.MBeanStatusException;

/**
 * Checks the state of a replication agent MBean
 * @author joerg
 *
 */
public class ReplicationAgentStatusProvider implements HealthStatusProvider {

	Logger log = LoggerFactory.getLogger(ReplicationAgentStatusProvider.class);

	private ReplicationAgentStatusOptions options;
	

	
	protected ReplicationAgentStatusProvider (ReplicationAgentStatusOptions options) {
		this.options = options;
	}
	
	/**
	 * Return the healthstatus of the agent
	 */
	public HealthStatus getHealthStatus() {
		String message = "okok";
		String providerName = this.getClass().getName();
		int status = HS_OK;
		try {
			providerName = "Replication Agent " + getAgentName();
		} catch (MBeanStatusException e) {
			status = HS_WARN;
		}
		
		return new HealthStatus (status,message,providerName);
	}
	
	@org.apache.felix.scr.annotations.Activate
	protected void Activate (ComponentContext ctx) {
		log.info ("Activating " + options.getObjectName().toString());
	}
	
	@org.apache.felix.scr.annotations.Deactivate
	protected void Deactivate (ComponentContext ctx) {
		log.info ("Dectivating " + options.getObjectName().toString());
	}

	// private
	
	private long getQueueLength() throws MBeanStatusException {
		Object r = getValue ("QueueNumEntries");
		if (r != null) {
			return (Long) r;
		} else {
			return -1;
		}
	}
	
	
	private String getAgentName () throws MBeanStatusException {
		Object r = getValue ("Id");
		if (r != null) {
			return (String ) r;
		}
		else {
			return "";
		}
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
