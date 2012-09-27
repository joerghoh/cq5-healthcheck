package de.joerghoh.cq5.healthcheck.providers;

import java.lang.management.ManagementFactory;
import java.util.Map;

import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.ReflectionException;

import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.ValueFormatException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.joerghoh.cq5.healthcheck.HealthStatus;
import de.joerghoh.cq5.healthcheck.HealthStatusProvider;

public class MBeanStatusProvider implements HealthStatusProvider {

	private Logger log = LoggerFactory.getLogger(MBeanStatusProvider.class);
	
	private ObjectName name;
	private Map<String,Property> props;
	
	private MBeanServer server = ManagementFactory.getPlatformMBeanServer();
	
	public HealthStatus getHealthStatus() {
		String text;
		try {
			String attribute = props.get("JMXproperty").getString();
			String mbeanValue = server.getAttribute (name,attribute).toString();
			return new HealthStatus(calculateStatus(mbeanValue), attribute + " = "+ mbeanValue, name.toString());
		} catch (ValueFormatException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (RepositoryException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
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
		return new HealthStatus(WARN, "some failure", name.toString());
	}

	
	private int calculateStatus(String value) throws ValueFormatException, RepositoryException {
		
		int status = OK;
		if (calcStatus2(value,"critical",CRITICAL) != 0 ) {
			status =  CRITICAL;
		}
		if (calcStatus2(value,"warn",WARN) != 0 ) {
			status =  WARN;
		}
		log.info("Calculated status " + status + " (value="+value+")");
		return status;
	}
	
	private int calcStatus2(String value, String stringLevel, int intLevel) throws ValueFormatException, RepositoryException {
		if (props.containsKey(stringLevel+".==")) {
			long limit = props.get(stringLevel+".==").getLong();
			int i = Integer.parseInt(value);
			if (i == limit) {
				return intLevel;
			}
		} else if (props.containsKey(stringLevel+".>")) {
			long limit = props.get(stringLevel+".>").getLong();
			int i = Integer.parseInt(value);
			if (i > limit) {
				return intLevel;
			}
		} else if (props.containsKey(stringLevel+".<")) {
			long limit = props.get(stringLevel+".<").getLong();
			int i = Integer.parseInt(value);
			if (i < limit) {
				return intLevel;
			}
		} else if (props.containsKey(stringLevel+".equal")) {
			String match = props.get(stringLevel+".equal").getString();
			if (value.equals(match)) {
				return intLevel;
			}
		} else if (props.containsKey(stringLevel+".notequal")) {
			String match = props.get(stringLevel+".notequal").getString();
			if (!value.equals(match)) {
				return intLevel;
			}
		}
		return 0;
	}
	
	public MBeanStatusProvider (ObjectName mbean,Map<String,Property> properties) {
		name = mbean;
		props = properties;
	}
	
}
