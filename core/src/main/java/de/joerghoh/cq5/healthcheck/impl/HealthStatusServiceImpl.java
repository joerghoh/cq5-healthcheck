package de.joerghoh.cq5.healthcheck.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.Service;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.joerghoh.cq5.healthcheck.HealthStatus;
import de.joerghoh.cq5.healthcheck.HealthStatusProvider;
import de.joerghoh.cq5.healthcheck.HealthStatusService;
import de.joerghoh.cq5.healthcheck.OverallHealthStatus;

/**
 * 
 * @author joerg
 * 
 */
@Component(metatype=true,immediate=true)
@Service(value=HealthStatusService.class)
@Reference(name="healthStatusProvider",referenceInterface=HealthStatusProvider.class,cardinality=ReferenceCardinality.OPTIONAL_MULTIPLE, policy=ReferencePolicy.DYNAMIC)
public class HealthStatusServiceImpl implements HealthStatusService {

	 private List<HealthStatusProvider> providers = new ArrayList<HealthStatusProvider>();
	 private final Logger log = LoggerFactory.getLogger(HealthStatusServiceImpl.class);
    
	 
	 /**
	  * Get overall status
	  */
	 public OverallHealthStatus getOverallStatus() {
		
		 int finalStatus = 0;
		 List<HealthStatus> results = new ArrayList<HealthStatus>(10);
		
		 
		 for (HealthStatusProvider p: providers) {
			 HealthStatus s = p.getHealthStatus();
			 if (s.getStatus() > finalStatus) {
				 finalStatus = s.getStatus();
			 }
			 results.add(s);
		 }
		 
		 log.info("Processed " + results.size() + " providers");
		 return new OverallHealthStatus (finalStatus,results);
	 }
	 
	 
	 
	 /** helper routines**/
	 
	 protected void bindHealthStatusProvider (HealthStatusProvider provider) {
		 providers.add(provider);
	 }
	 
	 protected void unbindHealthStatusProvider (HealthStatusProvider provider) {
		 providers.remove(provider);
	 }
	

	
}
