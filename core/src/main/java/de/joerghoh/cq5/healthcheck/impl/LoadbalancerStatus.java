


package de.joerghoh.cq5.healthcheck.impl;

import java.io.IOException;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.day.cq.jcrclustersupport.ClusterAware;

import de.joerghoh.cq5.healthcheck.HealthStatusService;

/**
 * A servlet, which returns the actual status suitable for a loadbalancer.
 * 
 * This code allows to specify a cluster strategy:
 *  * ActiveActive: All cluster nodes are eligable to return "OK"
 *  * ActivePassive: Only the master is eligable to return "OK", slaves always return "NoOK".
 * 
 * In any case also the OverallStatus of the HealthStatusService is used to identify the status.
 * If the status is != OK, then "notOK" is returned.
 * 
 * .
 * @author joerg
 *
 */

@Component(immediate=true,metatype=true)
@Service(value=javax.servlet.Servlet.class)
@Properties({
	@Property(name="sling.servlet.methods", value="GET"),
	@Property(name="sling.servlet.paths", value={"/bin/loadbalancer"})
})

public class LoadbalancerStatus extends SlingSafeMethodsServlet implements ClusterAware {

	@Reference
	private HealthStatusService statusService;
	
	private static final String DEFAULT_LB_STRATEGY="ActivePassive";
	@Property (value=DEFAULT_LB_STRATEGY,
			name="Clustering strategy",
			description="Specify your clustering strategy to instruct the loadbalancer. " +
					"Currently 'ActivePassive' and 'ActiveActive' are supported.")
	private static final String PROPERTY_LB_STRATEGY = "strategy";
	
	
	private boolean iAmMaster = false;
	private String loadbalancerStrategy;
	private Logger log = LoggerFactory.getLogger(LoadbalancerStatus.class);
	
	private static final long serialVersionUID = -8012558085365805331L;
	
	public void doGet (SlingHttpServletRequest request, SlingHttpServletResponse response) {
		
		try {
		String systemStatusString = statusService.getOverallStatus().getStatus();
		boolean allOK = systemStatusString.equals("OK");
		boolean statusOK = false;
		
		if (loadbalancerStrategy.equals("ActivePassive")) {
			statusOK = iAmMaster & allOK;
		} else if (loadbalancerStrategy.equals("ActiveActive")) {
			statusOK = allOK;
		} else {
			statusOK = false;
			log.error ("Invalid loadbalancer strategy set: " + loadbalancerStrategy);
		}
		
		response.setContentType("text/html");
		if (statusOK) {
			response.getOutputStream().print("OK");
		} else {
			response.getOutputStream().print("NotOK");
		}
		response.getOutputStream().flush();
		} catch (IOException e)  {
			log.warn("Cannot write to output: " + e.getMessage());
		}
	}
	
	
	/** ClusterAware impl **/
	
	public void bindRepository(String repositoryId, String clusterId, boolean isMaster) {
	    iAmMaster = isMaster;
	  }


	public void unbindRepository() {
		// TODO Auto-generated method stub
		
	}
	
	/** SCR **/
	
	@org.apache.felix.scr.annotations.Activate
	public void Activate (ComponentContext ctx) {
		loadbalancerStrategy = PropertiesUtil.toString(ctx.getProperties().get(PROPERTY_LB_STRATEGY),DEFAULT_LB_STRATEGY);
		log.info("Using loadbalancer strategy "+loadbalancerStrategy);
	}

}
