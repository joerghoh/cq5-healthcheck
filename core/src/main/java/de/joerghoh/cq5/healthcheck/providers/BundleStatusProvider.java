package de.joerghoh.cq5.healthcheck.providers;

import java.util.Dictionary;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;

import de.joerghoh.cq5.healthcheck.HealthStatus;
import de.joerghoh.cq5.healthcheck.HealthStatusProvider;

/**
 * This service checks if all non-fragment bundles are up; if at least 1 bundle is not 
 * active, the status WARN is reported
 * @author joerg
 *
 */
@Component
@Service (value=HealthStatusProvider.class)
public class BundleStatusProvider implements HealthStatusProvider {

	
	private BundleContext bctx;
	
	public HealthStatus getHealthStatus() {
		 Bundle[] bundles = bctx.getBundles();
		 int notActiveBundleCount = 0;
		 for (Bundle b: bundles) {
			
			 Dictionary<String,String> headers = b.getHeaders();
			 boolean isFragment = (headers.get("Fragment-Host") != null);
			 boolean isActive = (b.getState() == Bundle.ACTIVE);
			 
			 if (!isFragment && !isActive) {
				 notActiveBundleCount++;
			 }
			 
			 
		 }
		 
		 String statusMessage = notActiveBundleCount + " bundles not active";
		 int status = HS_WARN;
		 if (notActiveBundleCount == 0) {
			 status = HS_OK;
			 statusMessage = "all bundles active";
		 }
		 String providerName = this.getClass().getName();
		 return new HealthStatus(status,statusMessage,providerName);
		 
	}

	@Activate
	protected void activate (ComponentContext ctx) {
		bctx = ctx.getBundleContext();
	}
	
}
