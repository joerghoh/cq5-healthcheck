package de.joerghoh.cq5.healthcheck.providers;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;

import de.joerghoh.cq5.healthcheck.HealthStatus;
import de.joerghoh.cq5.healthcheck.HealthStatusProvider;

@Component(immediate=true)
@Service(value= HealthStatusProvider.class)
public class TotalRequestsProvider extends AbstractMBeanProvider{

    @Override
    public HealthStatus getHealthStatus() {
        
        long req = getMBeanLongValue("org.apache.sling:type=engine,service=RequestProcessor","RequestsCount");
        
        return new HealthStatus (HS_OK,"Requests:" + req,TotalRequestsProvider.class.getName());
    }

}
