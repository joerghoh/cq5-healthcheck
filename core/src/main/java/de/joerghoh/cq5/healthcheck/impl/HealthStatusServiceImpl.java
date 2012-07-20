package de.joerghoh.cq5.healthcheck.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.felix.scr.annotations.Component;
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
public class HealthStatusServiceImpl implements HealthStatusService {

	 private HealthStatusProvider[] providers = new HealthStatusProvider[0];
	 private HealthStatusTracker tracker;
	 private final Logger log = LoggerFactory.getLogger(HealthStatusServiceImpl.class);
    
	 
	 /**
	  * Get overall status
	  */
	 public OverallHealthStatus getOverallStatus() {
		
		 int finalStatus = 0;
		 List<HealthStatus> results = new ArrayList<HealthStatus>(10);
		 
		 /** first create a copy of the provider list, it might change over time,
		  *  while we still query the HealthStatusProviders.
		  *  **/
		 HealthStatusProvider[] copy;
		 synchronized (this) {
			 copy = new HealthStatusProvider[providers.length];
			 for (int i=0; i<providers.length;i++) {
				 copy[i] = providers[i];
			 }
		 }
		 
		 for (HealthStatusProvider p: copy) {
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
	
	 /**
	  * add a new HealthStatusProvider
	  * @param provider
	  * called only by the internal HealthStatusTracker
	  */
	 protected synchronized void addProvider (HealthStatusProvider provider) {
		 ArrayList<HealthStatusProvider> p = new ArrayList<HealthStatusProvider>(
				 Arrays.asList(providers));
		 p.add(provider);
		 providers = p.toArray(new HealthStatusProvider[p.size()]);
	 }
	 
	 /**
	  * remove a HealthStatusProvider
	  * @param provider
	  * called only the internal HealthStatusTracker
	  */
	 protected synchronized void removeProvider (HealthStatusProvider provider) {
		 ArrayList<HealthStatusProvider> p = new ArrayList<HealthStatusProvider>(
				 Arrays.asList(providers));
		 p.remove(provider);
		 providers = p.toArray(new HealthStatusProvider[p.size()]);   
	 }

	/** SCR **/
	
	protected void activate (ComponentContext ctx) {
		tracker = new HealthStatusTracker(ctx,this);
		tracker.open();
		log.info("HealthStatusServiceImpl activted");
	}
	
	protected void deactivate (ComponentContext ctx) {
		if (tracker != null) {
			tracker.close();
		}
		tracker = null;
		
	}
	
	/**
	 * keep track of all services implementing HealthStatusProvider
	 * @author joerg
	 *
	 */
	
	private class HealthStatusTracker extends ServiceTracker {
		
		private HealthStatusServiceImpl s;
		
		public HealthStatusTracker (ComponentContext ctx, HealthStatusServiceImpl service) {
			super (ctx.getBundleContext(),HealthStatusProvider.class.getName(),null);
			s = service;
		}
		
        public Object addingService(ServiceReference reference) {
            HealthStatusProvider provider = (HealthStatusProvider) super.addingService(reference);
            s.addProvider (provider);
            return provider;
        }

        @Override
        public void removedService(ServiceReference reference, Object service) {
            if (service instanceof HealthStatusProvider) {
                s.removeProvider((HealthStatusProvider) service);
            }
            super.removedService(reference, service);
        }
		
	}
	
}
