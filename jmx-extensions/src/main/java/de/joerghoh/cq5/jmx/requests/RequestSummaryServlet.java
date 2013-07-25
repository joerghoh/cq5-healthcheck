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

import java.lang.management.ManagementFactory;
import java.util.Set;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.commons.json.io.JSONWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The RequestSummaryServlet exports the data collected in the MBeans to JSON
 * 
 * @author joerg
 */
@Component
@Service
@Properties({
	@Property(name = "sling.servlet.methods", value = "GET", propertyPrivate=true),
	@Property(name = "sling.servlet.selectors", value = "json", propertyPrivate=true),
	@Property(name = "sling.servlet.resourceTypes", value = "healthcheck/servlet/requestinfo", propertyPrivate=true) 
})
public class RequestSummaryServlet extends SlingAllMethodsServlet {

	private static final long serialVersionUID = 4721202027009891341L;
	private Logger log = LoggerFactory.getLogger(RequestSummaryServlet.class);
	private static final String MBEANFILTER = "de.joerghoh.cq5.jmx.requests.mime:type=*";

	@Override
	public void doGet(SlingHttpServletRequest request,
			SlingHttpServletResponse response) {

		response.setContentType("application/json");
		MBeanServer server = ManagementFactory.getPlatformMBeanServer();

		ObjectName query;
		try {
			JSONWriter result = new JSONWriter(response.getWriter()).array();
			query = new ObjectName(MBEANFILTER);
			Set<ObjectName> mbeans = server.queryNames(query, null);
			// log.info("Retrieving information from " + mbeans.size() +
			// " mbeans");
			for (ObjectName mbean : mbeans) {
				String mbeanName = mbean.toString();
				String requestCounter = String.valueOf(server.getAttribute(
						mbean, "RequestCounter"));
				String totalDuration = String.valueOf(server.getAttribute(
						mbean, "TotalRequestDuration"));
				String mimeType = (String) server.getAttribute(mbean,
						"MimeType");
				result.object().key("MbeanName").value(mbeanName)
						.key("RequestCounter").value(requestCounter)
						.key("TotalRequestDuration").value(totalDuration)
						.key("MimeType").value(mimeType).endObject();
			}
			result.endArray();

		} catch (Exception e) {
			log.error("Cannot query mbeans", e);
		}
	}
}
