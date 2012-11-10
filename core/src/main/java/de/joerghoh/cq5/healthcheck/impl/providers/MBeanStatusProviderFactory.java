/*
 * Copyright 2012 Jörg Hoh, Alexander Saar, Markus Haack
 * 
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package de.joerghoh.cq5.healthcheck.impl.providers;

import java.lang.management.ManagementFactory;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingConstants;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.resource.ValueMap;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.joerghoh.cq5.healthcheck.HealthStatusProvider;

/**
 * The MBeanStatusProvider is responsible to maintain the MBeanStatusProvider
 * services as they are configured in the repository. For all MBeans, for which
 * a configuration exist, healthstatus services are created which monitor these
 * mbeans.
 * 
 * @author joerg
 */
@Component(immediate = true)
@Service()
@Property(name = "event.topics", value = {
		"org/apache/sling/api/resource/Resource/*",
		"org/osgi/framework/BundleEvent/*" })
public class MBeanStatusProviderFactory implements EventHandler {

	private static String CONFIG_PATH = "/etc/healthcheck/mbeans";
	private static String ENABLED_PROPERTY = "enabled";
	private Logger log = LoggerFactory
			.getLogger(MBeanStatusProviderFactory.class);

	private BundleContext bundleContext;

	private Map<String, ServiceRegistration> registeredServices = new ConcurrentHashMap<String, ServiceRegistration>();

	@Reference
	private ResourceResolverFactory rrfac;
	private ResourceResolver adminResolver;

	private MBeanServer server = ManagementFactory.getPlatformMBeanServer();

	@Activate
	protected void activate(ComponentContext ctx) throws RepositoryException {
		bundleContext = ctx.getBundleContext();
		try {
			adminResolver = rrfac.getAdministrativeResourceResolver(null);
			loadConfig();
		} catch (LoginException e) {
			log.error("Cannot login into repo", e);
		}
	}

	@Deactivate
	protected void deactivate() {
		if (adminResolver != null) {
			adminResolver.close();
		}
		deactivateAllServices();
	}

	/**
	 * The event handler for resource changes used to track config changes in
	 * the repository and also to modifications to the mbeans (services are used
	 * to provide mbeans, so we monitor for all bundle events)
	 */
	public void handleEvent(org.osgi.service.event.Event event) {

		/**
		 * On a bundle event we validate all services, as a bundle event might
		 * have added/removed new mbeans, so we do * validate that for all
		 * loaded healthstatus services the corresponding mbean exists * check
		 * the config, if we can load an additional healthstatus services,
		 * because a required mbean just arrived
		 */

		if (event.getTopic().startsWith("org/osgi/framework/BundleEvent/")) {
			try {
				log.info("Validating config");
				validateRunningServices();
				loadConfig();
			} catch (Exception e) {
				log.error("Cannot handle exception ", e);
			}
		}

		final Object p = event.getProperty(SlingConstants.PROPERTY_PATH);
		final String path;
		if (p != null && p instanceof String) {
			path = (String) p;
		} else {
			// not a string path or null, ignore this event
			return;
		}
		if (!path.startsWith(CONFIG_PATH)) {
			// it happened outside of our interest -- ignore
			return;
		}
		try {
			// potentially config has changed.

			Resource r = adminResolver.getResource(path);
			log.info("Config change detected at " + path);
			if (SlingConstants.TOPIC_RESOURCE_ADDED.equals(event.getTopic())) {
				createService(r);
			} else if (SlingConstants.TOPIC_RESOURCE_CHANGED.equals(event
					.getTopic())) {
				unregisterService(path);
				createService(r);
			} else if (SlingConstants.TOPIC_RESOURCE_REMOVED.equals(event
					.getTopic())) {
				unregisterService(path);
			}
		} catch (RepositoryException e) {
			log.error("Cannot handle event ", e);
		}
	}

	/**
	 * Checks all running services if their corresponding mbeans are still
	 * there. If they're no more present, unregister the respective service
	 */
	private void validateRunningServices() {
		final Iterator<String> services = registeredServices.keySet()
				.iterator();
		while (services.hasNext()) {
			String path = services.next(); // the path of the resource
											// describing this service
			Resource r = adminResolver.getResource(path);
			ObjectName mbean = buildObjectName(r);
			if (mbean != null) {
				if (!mbeanExists(mbean)) {
					unregisterService(path);
				}
			}

		}
	}

	private void loadConfig() throws PathNotFoundException, RepositoryException {
		loadNodes(adminResolver.getResource(CONFIG_PATH));
	}

	/**
	 * Recursively iterate through all resources and create services where
	 * appropriate
	 * 
	 * @param res
	 * @throws RepositoryException
	 */
	private void loadNodes(Resource res) throws RepositoryException {

		if (isMbeanDefinition(res)) {
			createService(res);
		}
		Iterator<Resource> children = res.listChildren();
		while (children.hasNext()) {
			Resource r = children.next();
			loadNodes(r);
		}
	}

	/**
	 * Try to instantiate a healthStatusProvider on a definition
	 * 
	 * @param resource
	 *            the resource where the definition is stored
	 * @return
	 * @throws RepositoryException
	 */
	private void createService(Resource resource) throws RepositoryException {

		ValueMap props = ResourceUtil.getValueMap(resource);
		if (!isMbeanDefinition(resource)) {
			return;
		}
		if (registeredServices.get(resource.getPath()) != null) {
			log.debug("Trying to register already registered service for path "
					+ resource.getPath());
			return;
		}
		ObjectName mbean = buildObjectName(resource);
		if (mbean != null) {
			String mbeanName = mbean.toString();
			if (mbeanExists(mbean)) {

				log.info("Instantiate healtcheck for MBean " + mbeanName);

				MBeanStatusProvider msp = new MBeanStatusProvider(mbean, props);
				Dictionary<String, String> params = new Hashtable<String, String>();
				// params.put(Constants.SERVICE_PID, pid );
				params.put(Constants.SERVICE_DESCRIPTION,
						"Statusprovider for mbean " + mbeanName);

				ServiceRegistration service = registerService(msp, params);
				registeredServices.put(resource.getPath(), service);

			}
		}
	}

	/**
	 * determine if a given resource denotes a valid service definition
	 * 
	 * @param resource
	 *            the resource which might define a service
	 * @return true if it's a valid definition
	 */
	private boolean isMbeanDefinition(Resource resource) {
		ValueMap props = ResourceUtil.getValueMap(resource);
		if (!props.containsKey(ENABLED_PROPERTY)) {
			return false;
		}

		String enabled = (String) props.get(ENABLED_PROPERTY);
		if (!(enabled.equals("true") || enabled.equals("yes"))) {
			return false;
		}
		return true;
	}

	private ObjectName buildObjectName(Resource resource) {
		String mbeanName = resource.getName().replaceFirst("_", ":");
		ObjectName mbean = null;
		try {
			mbean = new ObjectName(mbeanName);
		} catch (MalformedObjectNameException e) {
			log.error("Cannot create ObjectName " + mbeanName, e);
		} catch (NullPointerException e) {
			log.error("Cannot create ObjectName " + mbeanName, e);
		}
		return mbean;
	}

	private boolean mbeanExists(ObjectName mbean) {

		Set<ObjectName> beans = server.queryNames(mbean, null);
		return (beans.size() == 1);
	}

	// ////////////////////////////////////////////////////
	// helper to register & unregister services

	private ServiceRegistration registerService(MBeanStatusProvider service,
			Dictionary<String, String> params) {
		return bundleContext.registerService(
				HealthStatusProvider.class.getName(), service, params);
	}

	public void deactivateAllServices() {
		Iterator<ServiceRegistration> i = registeredServices.values()
				.iterator();
		while (i.hasNext()) {
			ServiceRegistration sr = i.next();
			sr.unregister();
		}
		registeredServices.clear();

	}

	/**
	 * unregister a service defined by a special node
	 * 
	 * @param resource
	 *            the resource defining the service configuration
	 */
	private void unregisterService(String path) {
		ServiceRegistration r = registeredServices.get(path);
		if (r != null) {
			r.unregister();
			registeredServices.remove(path);
		}
	}
}
