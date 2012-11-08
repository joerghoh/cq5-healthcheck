package de.joerghoh.cq5.healthcheck.impl.providers;

import java.lang.management.ManagementFactory;
import java.util.Iterator;

import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.ReflectionException;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.sling.api.resource.ValueMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.joerghoh.cq5.healthcheck.HealthStatus;
import de.joerghoh.cq5.healthcheck.HealthStatusProvider;

public class MBeanStatusProvider implements HealthStatusProvider {

	private Logger log = LoggerFactory.getLogger(MBeanStatusProvider.class);

	private ObjectName mbean;
	private ValueMap properties;

	private String statusMessage = "";

	private MBeanServer server = ManagementFactory.getPlatformMBeanServer();

	/**
	 * Constructor for MBeanStatusProvider.
	 * 
	 * @param mbean
	 * @param properties
	 */
	public MBeanStatusProvider(ObjectName mbean, ValueMap properties) {
		this.mbean = mbean;
		this.properties = properties;
	}

	/**
	 * is called whenever the status must be calculated
	 */
	public HealthStatus getHealthStatus() {
		int status = getStatus();
		return new HealthStatus(status, statusMessage, mbean.toString());
	}

	/**
	 * calculate the overall status of the service
	 * 
	 * @return the overall status
	 */
	private int getStatus() {
		int accumulatedStatus = OK;
		Iterator<String> iter = properties.keySet().iterator();
		statusMessage = "";
		while (iter.hasNext()) {
			String propertyName = iter.next();

			/*
			 * value might be: "my.JMX.propertyName.warn.>", and we need the triple of "jmxAttributeName","level" and "type of comparison".
			 * The jmxAttributeName might contain dots, so we need to reverse to use String.split()
			 */

			final String key = new StringBuffer(propertyName).reverse().toString(); // revert
																					// it
			final String[] split = key.split("\\.", 3);
			if (split.length != 3) {
				// key does not match the needed configuration triple
				continue;
			}
			final String comparisonAttributeName = new StringBuffer(split[2]).reverse().toString();
			// what would be the statusCode if we have a match?
			final String comparisonLevel = new StringBuffer(split[1]).reverse().toString();
			int statusCode = OK;
			if (comparisonLevel.equals("warn")) {
				statusCode = WARN;
			} else if (comparisonLevel.equals("critical")) {
				statusCode = CRITICAL;
			} else {
				log.warn("Ignoring property (invalid level): " + propertyName);
				continue;
			}
			final String comparisonOperation = new StringBuffer(split[0]).reverse().toString();

			// retrieve the long value for comparison
			final String comparisonValue = properties.get(propertyName, String.class);
			log.debug("comparsion {} for {}", propertyName, comparisonValue);

			// read the correct value via JMX
			Object jmxValueObj = getAttributeValue(comparisonAttributeName);
			if (jmxValueObj == null) {
				log.info("Ignoring property " + propertyName + "(no such a JMX attribute " + comparisonAttributeName + ")");
				continue;
			}
			log.debug("jmx value = {}", jmxValueObj);

			// do the comparison
			boolean match = false;
			try  {
				match = compareAttributeValue(comparisonOperation, comparisonValue, jmxValueObj);
			} catch (RuntimeException e) {
				log.info("Ignoring property (invalid value type): " + propertyName);
				continue;
			}

			// if we have a WARN or CRITICAL state for this value, report it
			if (match) {
				if (statusMessage.length() > 0) {
					statusMessage += ", ";
				}
				statusMessage += comparisonAttributeName + " = " + (jmxValueObj.getClass().isArray() ? ArrayUtils.toString(jmxValueObj) : jmxValueObj.toString());
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
	 * 
	 * @param attributeName
	 * @return
	 */
	private Object getAttributeValue(String attributeName) {
		try {
			return server.getAttribute(mbean, attributeName);
		} catch (AttributeNotFoundException e) {
			e.printStackTrace();
		} catch (InstanceNotFoundException e) {
			e.printStackTrace();
		} catch (MBeanException e) {
			e.printStackTrace();
		} catch (ReflectionException e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * compare helper method to compare the different types of attribute values
	 * 
	 * @param comparisonOperation
	 * @param comparisonValue
	 * @param jmxValueObj
	 * @return
	 */
	private boolean compareAttributeValue(final String comparisonOperation, final String comparisonValue, Object jmxValueObj) {
		boolean match = false;
		if (jmxValueObj instanceof Long) { // first check for plain value
			final long jmxLongValue = Long.parseLong(jmxValueObj.toString());
			final long comparisonLongValue = NumberUtils.createLong(comparisonValue);
			if (comparisonOperation.equals(">")) {
				match = (jmxLongValue > comparisonLongValue);
			} else if (comparisonOperation.equals("==")) {
				match = (jmxLongValue == comparisonLongValue);
			} else if (comparisonOperation.equals("<")) {
				match = (jmxLongValue < comparisonLongValue);
			} else {
				log.warn("Can not compare long values {} and {}", jmxLongValue, comparisonLongValue);
				throw new RuntimeException();
			}
		} else if (jmxValueObj instanceof long[]) { // second check for array
			final long[] jmxLongArrayValue = (long[]) jmxValueObj;
			final long comparisonLongValue = NumberUtils.createLong(comparisonValue);
			if (comparisonOperation.equals(">")) {
				match = (NumberUtils.max(jmxLongArrayValue) > comparisonLongValue);
			} else if (comparisonOperation.equals("==")) {
				// TODO maybe it makes more sense for == to compare all array values?
				match = ArrayUtils.contains(jmxLongArrayValue, comparisonLongValue);
			} else if (comparisonOperation.equals("<")) {
				match = (NumberUtils.min(jmxLongArrayValue) < comparisonLongValue);
			} else {
				log.warn("Can not compare long array values {} and {}", jmxLongArrayValue, comparisonLongValue);
				throw new RuntimeException();
			}
		} else if (jmxValueObj instanceof String) { // third check for String values
			final String jmxStringValue = jmxValueObj.toString();
			if (comparisonOperation.equals("equals")) {
				match = (comparisonValue.equals(jmxStringValue));
			} else if (comparisonOperation.equals("notequals")) {
				match = (!comparisonValue.equals(jmxStringValue));
			} else {
				log.warn("Can not compare String values {} and {}", jmxStringValue, comparisonValue);
				throw new RuntimeException();
			}
		} else if (jmxValueObj instanceof Boolean) {
			final boolean jmxBooleanValue = (Boolean) jmxValueObj;
			final boolean comparisonBooleanValue = Boolean.parseBoolean(comparisonValue);
			if (comparisonOperation.equals("equals")) {
				match = (comparisonBooleanValue == jmxBooleanValue);
			} else if (comparisonOperation.equals("==")) {
				match = (comparisonBooleanValue == jmxBooleanValue);
			} else if (comparisonOperation.equals("notequals")) {
				match = (comparisonBooleanValue != jmxBooleanValue);
			} else {
				log.warn("Can not compare boolean values {} and {}", jmxBooleanValue, comparisonBooleanValue);
				throw new RuntimeException();
			}
		} else {
			log.warn("Can not compare jmx attribute value {} with {}", jmxValueObj, comparisonValue);
			throw new RuntimeException();
		}
		return match;
	}
}
