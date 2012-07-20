package de.joerghoh.cq5.healthcheck.providers;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;


import de.joerghoh.cq5.healthcheck.HealthStatus;
import de.joerghoh.cq5.healthcheck.HealthStatusProvider;

@Component(immediate=true)
@Service(value= HealthStatusProvider.class)
public class SampleProvider implements HealthStatusProvider {

	public HealthStatus getHealthStatus() {
		
		return new HealthStatus (HS_OK,"Everything's fine",this.getClass().getName());
	}
	

}
