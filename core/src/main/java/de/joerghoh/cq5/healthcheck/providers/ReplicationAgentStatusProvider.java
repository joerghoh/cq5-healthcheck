package de.joerghoh.cq5.healthcheck.providers;

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

/**
 * Checks the state of a replication agent MBean
 * @author joerg
 *
 */
public class ReplicationAgentStatusProvider implements HealthStatusProvider {

	Logger log = LoggerFactory.getLogger(ReplicationAgentStatusProvider.class);
	ObjectName mbean;
	MBeanServer server;
	
	protected ReplicationAgentStatusProvider (MBeanServer server, ObjectName mbeanName) {
		mbean = mbeanName;
		this.server = server;
	}
	
	/**
	 * Return the healthstatus of the agent
	 */
	public HealthStatus getHealthStatus() {
		String providerName = "Replication Agent " + getAgentName();
		return new HealthStatus (HS_OK,"Alles in Ordnung",providerName);
	}
	
	@org.apache.felix.scr.annotations.Activate
	protected void Activate (ComponentContext ctx) {
		log.info ("Activating " + mbean.toString());
	}
	
	@org.apache.felix.scr.annotations.Deactivate
	protected void Deactivate (ComponentContext ctx) {
		log.info ("Dectivating " + mbean.toString());
	}

	// private
	
	private long getQueueLength() {
		Object r = getValue ("QueueNumEntries");
		if (r != null) {
			return (Long) r;
		} else {
			return -1;
		}
	}
	
	
	private String getAgentName () {
		Object r = getValue ("Id");
		if (r != null) {
			return (String ) r;
		}
		else {
			return "";
		}
	}
	
	
	private Object getValue (String property) {
       Object result = null;
		try {
			result =  server.getAttribute (mbean, property);
		} catch (AttributeNotFoundException e) {
			log.error("Cannot read property %s from MBean",property,e);
		} catch (InstanceNotFoundException e) {
			log.error("Cannot read property %s from MBean",property,e);
		} catch (MBeanException e) {
			log.error("Cannot read property %s from MBean",property,e);
		} catch (ReflectionException e) {
			log.error("Cannot read property %s from MBean",property,e);
		}
		return result;
	}
       
	
	
}
