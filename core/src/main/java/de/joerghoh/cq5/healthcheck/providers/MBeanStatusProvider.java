package de.joerghoh.cq5.healthcheck.providers;

import java.lang.management.ManagementFactory;
import java.util.Iterator;

import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.ReflectionException;

import org.apache.sling.api.resource.ValueMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.joerghoh.cq5.healthcheck.HealthStatus;
import de.joerghoh.cq5.healthcheck.HealthStatusProvider;

public class MBeanStatusProvider implements HealthStatusProvider {

	private Logger log = LoggerFactory.getLogger(MBeanStatusProvider.class);
	
	private ObjectName name;
	private ValueMap props;
	
	private String statusMessage = "";
	
	private MBeanServer server = ManagementFactory.getPlatformMBeanServer();
	
	/**
	 * is called whenever the status must be calculated
	 */
	public HealthStatus getHealthStatus() {
		int status = getStatus();
		return new HealthStatus(status, statusMessage, name.toString());
	}

	
	/**
	 * calculate the overall status of the service
	 * @return the overall status
	 */
	private int getStatus() {
		int accumulatedStatus = OK;
		Iterator<String> iter = props.keySet().iterator();
		statusMessage = "";
		while (iter.hasNext()) {
			String value = iter.next();
			
			/*
			 * value might be: "my.JMX.propertyName.warn.>", and we need the tripel of
			 * "jmxAttributeName","level" and "type of comparison".
			 * The jmxAttributeName might contain dots, so we need to reverse to use String.split()
			 */
			
			String key = new StringBuffer(value).reverse().toString(); // revert it
			String[] split = key.split("\\.", 3);
			if (split.length != 3) {
				continue;
			}
			String jmxAttributeName = new StringBuffer(split[2]).reverse().toString();
			String level = new StringBuffer(split[1]).reverse().toString();
			String comparison =  new StringBuffer(split[0]).reverse().toString();
			
			// read the correct value via JMX
			Object jmxValue = getAttributeValue (jmxAttributeName);
			if (jmxValue == null) {
				log.info("Ignoring property "+ value + "(no such a JMX attribute "+ jmxAttributeName + ")");
				continue;
			}
			String jmxStringValue = jmxValue.toString();
			long jmxLongValue = Long.parseLong (getAttributeValue (jmxAttributeName).toString());
			
			String stringValue = (String) props.get(value);
			long longValue = Long.parseLong(stringValue);
			
			log.debug("value="+value+",stringValue="+stringValue+",jmxStringValue="+jmxStringValue);
			
			// what would be the statusCode if we have a match?
			int statusCode = OK;
			if (level.equals("warn")) {
				statusCode = WARN;
			} else if (level.equals("critical")) {
				statusCode = CRITICAL;
			} else {
				log.info("Ignoring property (invalid level): "+ value);
				continue;
			}
			
			// do the comparison
			boolean match = false;
			if (comparison.equals(">")) {
				match = (jmxLongValue > longValue);
			} else if (comparison.equals("==")) {
				match = (jmxLongValue == longValue);
			} else if (comparison.equals("<")) {
				match = (jmxLongValue < longValue);
			} else if (comparison.equals("equals")) {
				match = (stringValue.equals(jmxStringValue));
			} else if (comparison.equals("notequals")) {
				match = (! stringValue.equals(jmxStringValue));
			} else {
				log.info("Ignoring property (invalid comparison): "+ value);
				continue;
			}
			
			// if we have a WARN or CRITICAL state for this value, report it
			if (match) {
				if (statusMessage.length() > 0 ) {
					statusMessage += ", ";
				}
				statusMessage += jmxAttributeName + "=" + jmxStringValue;
			}
			
			// if we have a match, update the overall status
			if (match && statusCode > accumulatedStatus) {
				accumulatedStatus = statusCode;
			}
		}
		
		return accumulatedStatus;
	}
	
	/**
	 * small wrapper method to read the value via JMX
	 * @param attributeName
	 * @return
	 */
	private Object getAttributeValue (String attributeName) {
		try {
			return server.getAttribute (name,attributeName);
		} catch (AttributeNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InstanceNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (MBeanException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ReflectionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	
	public MBeanStatusProvider (ObjectName mbean,ValueMap properties) {
		name = mbean;
		props = properties;
	}
	
}
