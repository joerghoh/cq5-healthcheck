package de.joerghoh.cq5.healthcheck.providers;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;

import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.ReflectionException;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.joerghoh.cq5.healthcheck.HealthStatusProvider;

@Component(immediate=true)
public class ReplicationAgentStatusFactory {
    
    private Logger log = LoggerFactory.getLogger(ReplicationAgentStatusFactory.class);
    private BundleContext bundleContext;
    private List<ServiceRegistration> registeredServices = new ArrayList<ServiceRegistration>(5);
    private MBeanServer server;

    private static final String queryString = "com.adobe.granite.replication:type=agent,id=*";
    
    @Activate
    protected void activate (ComponentContext ctx) {
        
    	bundleContext = ctx.getBundleContext();
        server = ManagementFactory.getPlatformMBeanServer();
        ObjectName query;
        try {
            query = new ObjectName (queryString);
            Set<ObjectName> agents = server.queryNames(query, null);
            log.error ("Found " + agents.size() + " agents");
            
            for (ObjectName agent: agents) {
                log.info("registering service for agent " + agent.toString());
                
                String agentName = getAgentName (agent);
                
                Dictionary<String,String> params = new Hashtable<String,String>();
                params.put(Constants.SERVICE_PID, agentName);
                params.put(Constants.SERVICE_DESCRIPTION, "replication agent : " + agentName);
                
                ReplicationAgentStatusProvider rasp = new ReplicationAgentStatusProvider (server, agent);
                
                ServiceRegistration sr = registerService (rasp,params);
                registeredServices.add(sr);
                
            }
            
        } catch (MalformedObjectNameException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (NullPointerException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (AttributeNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InstanceNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (MBeanException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ReflectionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        
    }
    
    @Deactivate
    protected void deactivate (ComponentContext ctx) {
    	for (ServiceRegistration sr: registeredServices) {
    		sr.unregister();
    	}
    }
    
    // helper to register services
    
    private ServiceRegistration registerService (ReplicationAgentStatusProvider service, Dictionary<String,String> params) {
    	return bundleContext.registerService(HealthStatusProvider.class.getName(), service, params);
    }
    
    private void unregisterService (ReplicationAgentStatusProvider service) {
    	
    }
    
    
    // private stuff
    
    private String getAgentName (ObjectName agent) throws AttributeNotFoundException, InstanceNotFoundException, MBeanException, ReflectionException {
    	return server.getAttribute(agent, "Id").toString();
    }
    

}
