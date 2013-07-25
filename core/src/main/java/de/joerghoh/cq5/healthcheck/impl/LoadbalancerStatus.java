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
package de.joerghoh.cq5.healthcheck.impl;

import java.io.IOException;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.jcr.api.SlingRepository;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.day.cq.jcrclustersupport.ClusterAware;

import de.joerghoh.cq5.healthcheck.StatusService;
import de.joerghoh.cq5.healthcheck.StatusCode;

/**
 * A servlet, which returns the actual status suitable for a loadbalancer.
 * 
 * Internally the HealthStatusService and the current clustering status is used
 * to determine the return code.
 * 
 * This code allows to specify a cluster strategy: 
 * * ActiveActive: All cluster nodes are eligable to return "OK" 
 * * ActivePassive: Only the master is eligable to return "OK", slaves always return "WARN".
 * 
 * In any case also the OverallStatus of the HealthStatusService is used to
 * identify the status. If the status is != OK, then "WARN" is returned and the
 * return code of this request is set to 500 (internal server error).
 * 
 * @author joerg
 */
@Component(immediate = true, metatype = true, label="Loadbalancer director service", description="Service managing the loadbalancer based on the healtchechk data ")
@Service(value = {javax.servlet.Servlet.class, com.day.cq.jcrclustersupport.ClusterAware.class})
@Properties({
		@Property(name = "sling.servlet.methods", value = "GET", propertyPrivate=true),
		@Property(name = "sling.servlet.resourceTypes", value = "healthcheck/servlet/loadbalancer", propertyPrivate=true) 
})
public class LoadbalancerStatus extends SlingSafeMethodsServlet implements
		ClusterAware {

	@Reference
	private StatusService statusService;

	@Reference
	private SlingRepository repository;
	
	private static final String DEFAULT_LB_STRATEGY = "ActivePassive";
	@Property(value = DEFAULT_LB_STRATEGY, description = "Specify your clustering strategy to instruct the loadbalancer. "
			+ "Currently 'ActivePassive' and 'ActiveActive' are supported.")
	private static final String PROPERTY_LB_STRATEGY = "strategy";

	private boolean iAmMaster = false;
	private String loadbalancerStrategy;
	private Logger log = LoggerFactory.getLogger(LoadbalancerStatus.class);

	private static final long serialVersionUID = -8012558085365805331L;
	

	@Override
	public void doGet(SlingHttpServletRequest request,
			SlingHttpServletResponse response) {

		try {
			StatusCode status = statusService.getStatus().getStatus();
			boolean allServicesOK = ( status == StatusCode.OK );
			boolean statusOK = false;

			if (loadbalancerStrategy.equals("ActivePassive")) {
				statusOK = iAmMaster && allServicesOK;
			} else if (loadbalancerStrategy.equals("ActiveActive")) {
				statusOK = allServicesOK;
			} else {
				statusOK = false;
				log.error("Invalid loadbalancer strategy set: "
						+ loadbalancerStrategy);
			}
			
			response.setContentType("text/html");
			if (statusOK) {
				response.setStatus(200);
				response.getOutputStream().print(StatusCode.OK.toString());
			} else {
				// in case of problems return an errorcode
				response.setStatus(500);
				response.getOutputStream().print(StatusCode.WARN.toString());
			}
			response.getOutputStream().flush();
		} catch (IOException e) {
			log.warn("Cannot write to output: " + e.getMessage());
		}
	}

	/** ClusterAware impl 
	 * 
	 * be cautious: this approach detects only changes to the cluster state,
	 * but the initial status must be detected via another mechanism.
	 * 
	 * **/

	public void bindRepository(String repositoryId, String clusterId,
			boolean isMaster) {
		iAmMaster = isMaster;
	}

	public void unbindRepository() {
		log.warn("Repository is unbound, this should not happen!");
	}

	/** SCR **/

	@Activate
	public void Activate(ComponentContext ctx) {
		loadbalancerStrategy = PropertiesUtil.toString(
				ctx.getProperties().get(PROPERTY_LB_STRATEGY),
				DEFAULT_LB_STRATEGY);
		log.info("Using loadbalancer strategy " + loadbalancerStrategy);
		String v = repository.getDescriptor("crx.cluster.master");
		iAmMaster = (v == null || Boolean.parseBoolean(v));
	}
}
