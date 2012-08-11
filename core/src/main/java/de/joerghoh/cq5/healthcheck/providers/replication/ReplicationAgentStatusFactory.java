package de.joerghoh.cq5.healthcheck.providers.replication;

import java.io.IOException;
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
import org.apache.felix.scr.annotations.Reference;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.joerghoh.cq5.healthcheck.HealthStatusProvider;

/**
 * Check the available replication agents via JMX and create a ReplicationAgentStatusProvider for each agent.
 * @author joerg
 *
 * TODO: dynamic detection of new/removed agents
 */
@Component(immediate=true)
public class ReplicationAgentStatusFactory {
    
    private Logger log = LoggerFactory.getLogger(ReplicationAgentStatusFactory.class);
    private BundleContext bundleContext;
    private List<ServiceRegistration> registeredServices = new ArrayList<ServiceRegistration>(5);
    private MBeanServer server;
    
    @Reference
    private ConfigurationAdmin configurationAdmin;

    private static final String queryString = "com.adobe.granite.replication:type=agent,id=*";
    
    @SuppressWarnings("unchecked")
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
                //log.info("registering service for agent " + agent.toString());
                
                String agentName = getAgentName (agent);
                String pid = this.getClass().getName() + "." + agentName;
                
                Dictionary<String,String> params = new Hashtable<String,String>();
                params.put(Constants.SERVICE_PID, pid );
                params.put(Constants.SERVICE_DESCRIPTION, "StatusProvider for replication agent " + agentName);
                
                Configuration config = configurationAdmin.getConfiguration(pid,null);
                Dictionary properties;
                if (config != null && config.getProperties() != null) {
                	properties = config.getProperties();
                } else {
                	properties = new Hashtable();
                	properties.put(ReplicationAgentStatusUtil.QUEUE_ERROR_LENGTH, "1000");
                	properties.put(ReplicationAgentStatusUtil.QUEUE_WARN_LENGTH,"100");
                }
                
                ReplicationAgentStatusOptions opt = new ReplicationAgentStatusOptions();
                opt.setMbeanServer(server);
                opt.setObjectName(agent);
                opt.setPid(pid);
                opt.setProperties(properties);
                ReplicationAgentStatusProvider rasp = new ReplicationAgentStatusProvider (opt);
                
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
		} catch (IOException e) {
			// thrown by access to configuration admin
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

    
    // private stuff
    
    private String getAgentName (ObjectName agent) throws AttributeNotFoundException, InstanceNotFoundException, MBeanException, ReflectionException {
    	return server.getAttribute(agent, "Id").toString();
    }
    

}
