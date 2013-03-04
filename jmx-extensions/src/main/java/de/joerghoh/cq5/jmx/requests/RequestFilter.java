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
package de.joerghoh.cq5.jmx.requests;

import java.io.IOException;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.felix.scr.annotations.Component;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.day.cq.wcm.api.Page;

/**
 * RequestFilter -- provide information about requests
 * 
 * @author joerg
 * 
 *         This service maintains a set of services to deliver information about
 *         incoming requests. These services are then exported as MBeans via JMX
 *         Whiteboard.
 */
@Component(immediate = true)
@Service
@Properties({ @Property(name = "sling.filter.scope", value = "request") })
public class RequestFilter implements Filter {

	private static String MBEAN_PREFIX = "de.joerghoh.cq5.jmx.requests";

	private Logger log = LoggerFactory.getLogger(RequestFilter.class);

	private Map<String, ServiceRegistration> services = new HashMap<String, ServiceRegistration>();

	private BundleContext bundleContext;

	private boolean shutdownInProgress = false;

	// livecycle stuff

	@Activate
	protected void activate(ComponentContext ctx) {
		bundleContext = ctx.getBundleContext();
		shutdownInProgress = false;
	}

	@Deactivate
	protected void deactivate() {
		shutdownInProgress = true;
		for (ServiceRegistration sr : services.values()) {
			sr.unregister();
		}
	}

	public void init(FilterConfig config) throws ServletException {

	}

	public void destroy() {

	}

	// implementation

	public void doFilter(ServletRequest request, ServletResponse response,
			FilterChain chain) throws IOException, ServletException {

		RequestInformationImpl rii = null;

		if (!shutdownInProgress) {
			SlingHttpServletRequest req = (SlingHttpServletRequest) request;
			Resource r = req.getResource();
			String contentType = req.getResponseContentType();
			Page p = r.adaptTo(Page.class);
			String templatePath = "";
			if (p != null && p.getTemplate() != null) {
				templatePath = p.getTemplate().getPath();
			} else {
				// not a page or no template specified
			}

			// If there is no service defined for this mime type yet, we
			// register it
			String designator = buildMBeanName(contentType, templatePath);
			if (!services.containsKey(designator)) {
				registerReportingService(designator, contentType);
			}

			// TODO cache service objects for performance???

			ServiceRegistration reg = services.get(designator);
			Object o = bundleContext.getService(reg.getReference());
			if (o instanceof RequestInformationImpl) {
				rii = (RequestInformationImpl) o;
			} else {
				log.error("Something went wrong with the retrieval of the service object for meban "
						+ designator);
				rii = null;
			}
		}

		// forward the request down the chain and collect timing information
		long t1 = System.currentTimeMillis();
		chain.doFilter(request, response);
		long t2 = System.currentTimeMillis();

		if (!shutdownInProgress && rii != null) {
			rii.update(t2 - t1);
		}

	}

	// helper

	/**
	 * Create a valid ObjectName for this specific combination
	 * 
	 * @param mimetype
	 *            -- the mimetype for the request
	 * @param templatePath
	 *            -- the path to the template (if provided)
	 * @return a valid name which can be converted to an ObjectName
	 */
	private String buildMBeanName(String mimetype, String templatePath) {
		if (templatePath.equals("")) {
			return MBEAN_PREFIX + ".mime:type=" + mimetype;
		} else {
			return MBEAN_PREFIX + ".template:template=" + templatePath;
		}

	}

	private synchronized ServiceRegistration registerReportingService(String mbeanName,
			String mimeType) {
		
		// we need to recheck, as in the meantime it could have been registered already
		if (!services.containsKey(mbeanName)) {

			RequestInformationImpl rii = new RequestInformationImpl(mimeType);
			Dictionary<String, String> props = new Hashtable<String, String>();
			props.put("jmx.objectname", mbeanName);

			log.debug("Registering mbean for " + mbeanName);

			ServiceRegistration reg = bundleContext.registerService(
					RequestInformationMBean.class.getName(), rii, props);
			services.put(mbeanName, reg);

			return reg;
		} else {
			return null;
		}

	}
}
