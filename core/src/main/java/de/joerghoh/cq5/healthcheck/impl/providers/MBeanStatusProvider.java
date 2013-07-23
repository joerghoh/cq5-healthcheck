/*
 * Copyright 2012 Jörg Hoh, Alexander Saar, Markus Haack
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package de.joerghoh.cq5.healthcheck.impl.providers;

import java.lang.management.ManagementFactory;
import java.util.Dictionary;
import java.util.Set;

import javax.jcr.RepositoryException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.joerghoh.cq5.healthcheck.Status;
import de.joerghoh.cq5.healthcheck.StatusCode;
import de.joerghoh.cq5.healthcheck.StatusProvider;

@Service
@Component(label = "MBean Status Provider Factory", metatype = true, configurationFactory = true, policy = ConfigurationPolicy.REQUIRE)
public class MBeanStatusProvider implements StatusProvider {

	private static final Logger log = LoggerFactory
			.getLogger(MBeanStatusProvider.class);

	private MBeanServer server = ManagementFactory.getPlatformMBeanServer();

	@Property
	private static String CATEGORY = "provider.category";
	private String category;
	
	@Property
	private static String MBEAN_NAME = "mbean.name";
	private String mbeanName;

	@Property
	private static String MBEAN_PROPERTY = "mbean.property";
	private String[] properties;

	@Property
	private static String MBEAN_PROVIDER_HINT = "mbean.providerHint";
	private String providerHint;
	private String providerName;

	private ObjectName mbean;
	private String statusMessage = "";

	/**
	 * @see de.joerghoh.cq5.healthcheck.HealthStatusProvider#getHealthStatus()
	 */
	public Status getStatus() {
		mbean = buildObjectName(mbeanName);
		if (mbean != null && mbeanExists(mbean)) {
			StatusCode status = calculateStatus();
			return new Status(status, statusMessage, providerName);
		} else {
			log.info("Cannot resolve mbean " + mbeanName);
			return new Status(StatusCode.UNKNOWN, "cannot find mbean",
					providerName);
		}
	}
	
	/**
	 * @see de.joerghoh.cq5.healthcheck.StatusProvider#getCategory()
	 */
	public String getCategory() {
		return category != null ? category : DEFAULT_CATEGORY;
	}

	@Activate
	protected void activate(ComponentContext ctx) throws RepositoryException {

		Dictionary<?, ?> props = ctx.getProperties();
		category = PropertiesUtil.toString(props.get(CATEGORY), null);
		mbeanName = PropertiesUtil.toString(props.get(MBEAN_NAME), null);
		properties = PropertiesUtil.toStringArray(props.get(MBEAN_PROPERTY));
		providerHint = PropertiesUtil.toString(props.get(MBEAN_PROVIDER_HINT),
				null);

		mbean = buildObjectName(mbeanName);
		if (mbean != null && mbeanExists(mbean)) {
			log.info("Instantiate healtcheck for MBean {}", mbeanName);
		} else {
			log.warn("Cannot instantiate healthcheck for MBean {}", mbeanName);
		}
		providerName = mbeanName;
		if (providerHint != null) {
			providerName += " (" + providerHint + ")";
		}
	}

	@Deactivate
	protected void deactivate() {

	}

	/**
	 * calculate the overall status of the service
	 * 
	 * @return the overall status
	 */

	private StatusCode calculateStatus() {
		StatusCode accumulatedStatus = StatusCode.OK;

		statusMessage = "";

		for (String property : properties) {

			/*
			 * value might be: "my.JMX.propertyName.warn.>", and we need the
			 * triple of "jmxAttributeName","level" and "type of comparison".
			 * The jmxAttributeName might contain dots, so we need to reverse to
			 * use String.split()
			 */

			final String key = new StringBuffer(property).reverse().toString(); // revert
																				// it
			final String[] split = key.split("\\.", 4);
			if (split.length != 4) {
				// key does not match the needed configuration triple
				continue;
			}
			final String comparisonAttributeName = new StringBuffer(split[3])
					.reverse().toString();

			// what would be the statusCode if we have a match?

			final String comparisonLevel = new StringBuffer(split[2]).reverse()
					.toString();
			StatusCode statusCode = StatusCode.OK;

			if (comparisonLevel.equalsIgnoreCase("warn")) {
				statusCode = StatusCode.WARN;
			} else if (comparisonLevel.equalsIgnoreCase("critical")) {
				statusCode = StatusCode.CRITICAL;
			} else {
				log.warn("Ignoring property (invalid level): " + property);
				continue;
			}

			final String comparisonOperation = new StringBuffer(split[1])
					.reverse().toString();

			// retrieve the long value for comparison
			final String comparisonValue = new StringBuffer(split[0]).reverse()
					.toString();
			log.debug("compare {} and {}", property, comparisonValue);

			// read the correct value via JMX
			Object jmxValueObj = getAttributeValue(comparisonAttributeName);
			if (jmxValueObj == null) {

				log.warn("Ignoring property " + property
						+ " (no such JMX attribute " + comparisonAttributeName
						+ ")");
				statusMessage = "Cannot resolve property or MBean";
				return StatusCode.WARN;
			}
			log.debug("jmx value = {}", jmxValueObj);

			// do the comparison
			boolean match = false;
			try {
				match = compareAttributeValue(comparisonOperation,
						comparisonValue, jmxValueObj);
			} catch (RuntimeException e) {
				log.info("Ignoring property (invalid value type): " + property);
				continue;
			}

			// if we have a WARN or CRITICAL state for this value, report it
			if (match) {
				if (statusMessage.length() > 0) {
					statusMessage += ", ";
				}
				statusMessage += comparisonAttributeName
						+ " = "
						+ (jmxValueObj.getClass().isArray() ? ArrayUtils
								.toString(jmxValueObj) : jmxValueObj.toString());
			}

			// if we have a match, update the overall status
			if (match && statusCode.compareTo(accumulatedStatus) > 0) {
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
		} catch (Exception e) {
			log.warn("Cannot read attribute " + attributeName + " from MBean "
					+ mbean);
		}
		return null;
	}

	/**
	 * compare helper method to compare the different types of attribute values
	 * supported types: ** long ** integer ** boolean ** string
	 * 
	 * @param comparisonOperation
	 * @param comparisonValue
	 * @param jmxValueObj
	 * @return
	 */
	private boolean compareAttributeValue(final String comparisonOperation,
			final String comparisonValue, Object jmxValueObj) {

		boolean match = false;

		if (jmxValueObj instanceof Long) { // first check for plain value
			final long jmxLongValue = Long.parseLong(jmxValueObj.toString());
			final long comparisonLongValue = NumberUtils
					.createLong(comparisonValue);
			if (comparisonOperation.equals(">")) {
				match = (jmxLongValue > comparisonLongValue);
			} else if (comparisonOperation.equals("==")) {
				match = (jmxLongValue == comparisonLongValue);
			} else if (comparisonOperation.equals("!=")) {
				match = (jmxLongValue != comparisonLongValue);
			} else if (comparisonOperation.equals("<")) {
				match = (jmxLongValue < comparisonLongValue);
			} else {
				log.warn("Can not compare long values {} and {}", jmxLongValue,
						comparisonLongValue);
				throw new RuntimeException();
			}

		} else if (jmxValueObj instanceof Integer) {
			final int jmxIntValue = Integer.parseInt(jmxValueObj.toString());
			final int comparisonIntValue = NumberUtils
					.createInteger(comparisonValue);
			if (comparisonOperation.equals(">")) {
				match = (jmxIntValue > comparisonIntValue);
			} else if (comparisonOperation.equals("==")) {
				match = (jmxIntValue == comparisonIntValue);
			} else if (comparisonOperation.equals("!=")) {
				match = (jmxIntValue != comparisonIntValue);
			} else if (comparisonOperation.equals("<")) {
				match = (jmxIntValue < comparisonIntValue);
			} else {
				log.warn("Can not compare int values {} and {}", jmxIntValue,
						comparisonIntValue);
				throw new RuntimeException();
			}

		} else if (jmxValueObj instanceof long[]) { // second check for array
			final long[] jmxLongArrayValue = (long[]) jmxValueObj;
			final long comparisonLongValue = NumberUtils
					.createLong(comparisonValue);
			if (comparisonOperation.equals(">")) {
				match = (NumberUtils.max(jmxLongArrayValue) > comparisonLongValue);
			} else if (comparisonOperation.equals("==")) {
				// TODO maybe it makes more sense for == to compare all array
				// values?
				match = ArrayUtils.contains(jmxLongArrayValue,
						comparisonLongValue);
			} else if (comparisonOperation.equals("<")) {
				match = (NumberUtils.min(jmxLongArrayValue) < comparisonLongValue);
			} else {
				log.warn("Can not compare long array values {} and {}",
						jmxLongArrayValue, comparisonLongValue);
				throw new RuntimeException();
			}
		} else if (jmxValueObj instanceof String) { // third check for String
													// values
			log.info("type comparison: string");
			final String jmxStringValue = jmxValueObj.toString();
			if (comparisonOperation.equals("equals")) {
				match = (comparisonValue.equals(jmxStringValue));
			} else if (comparisonOperation.equals("notequals")) {
				match = (!comparisonValue.equals(jmxStringValue));
			} else {
				log.warn("Can not compare String values {} and {}",
						jmxStringValue, comparisonValue);
				throw new RuntimeException();
			}
		} else if (jmxValueObj instanceof Boolean) {
			final boolean jmxBooleanValue = (Boolean) jmxValueObj;
			final boolean comparisonBooleanValue = Boolean
					.parseBoolean(comparisonValue);
			if (comparisonOperation.equals("equals")) {
				match = (comparisonBooleanValue == jmxBooleanValue);
			} else if (comparisonOperation.equals("==")) {
				match = (comparisonBooleanValue == jmxBooleanValue);
			} else if (comparisonOperation.equals("notequals")) {
				match = (comparisonBooleanValue != jmxBooleanValue);
			} else {
				log.warn("Can not compare boolean values {} and {}",
						jmxBooleanValue, comparisonBooleanValue);
				throw new RuntimeException();
			}
		} else {
			log.warn("Can not compare jmx attribute value {} with {}",
					jmxValueObj, comparisonValue);
			log.warn("jmxValueObj type = " + jmxValueObj.getClass().getName());
			throw new RuntimeException();
		}
		return match;
	}

	private ObjectName buildObjectName(String name) {
		ObjectName mbean = null;
		try {
			mbean = new ObjectName(name);
		} catch (MalformedObjectNameException e) {
			log.error("Cannot create ObjectName " + name, e);
		} catch (NullPointerException e) {
			log.error("Cannot create ObjectName " + name, e);
		}
		return mbean;
	}

	private boolean mbeanExists(ObjectName mbean) {
		Set<ObjectName> beans = server.queryNames(mbean, null);
		return (beans.size() == 1);
	}
}
