package de.joerghoh.cq5.healthcheck.providers;

import java.lang.management.ManagementFactory;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.EventListener;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.sling.api.SlingConstants;
import org.apache.sling.jcr.api.SlingRepository;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.joerghoh.cq5.healthcheck.HealthStatusProvider;

@Component(immediate=true)
public class MBeanStatusProviderFactory implements EventHandler {

	private static String configPath = "/etc/healthcheck/mbeans";
	private Logger log =  LoggerFactory.getLogger (MBeanStatusProviderFactory.class);
	
	private BundleContext bundleContext;
	
	private Map<String,ServiceRegistration> registeredServices = new HashMap<String,ServiceRegistration>();
	
	@Reference
	SlingRepository repo;
	
	
	private Session adminSession;
	
	@Activate
	protected void activate(ComponentContext ctx) throws RepositoryException {
		try {
			bundleContext = ctx.getBundleContext();
			adminSession = repo.loginAdministrative(null);
			initialLoad();
		} catch (RepositoryException e) {
			log.error ("Cannot login to repository",e);
			throw new RepositoryException (e);
		}
	}
	
	@Deactivate
	protected void deactivate() {
		if (adminSession != null) {
			adminSession.logout();
		}
		deactivateAllServices();
	}

	public void handleEvent(org.osgi.service.event.Event event) {
		final Object p = event.getProperty(SlingConstants.PROPERTY_PATH);
        final String path;
        if (p instanceof String) {
            path = (String) p;
        } else {
            // not a string path or null, ignore this event
            return;
        }
        if (!path.startsWith(configPath)) {
        	// it happened outside of our interest -- ignore
        	return;
        }
        try {
        	if (SlingConstants.TOPIC_RESOURCE_ADDED.equals(event.getTopic())) {
        		createService(adminSession.getNode(path));
        	} else if (SlingConstants.TOPIC_RESOURCE_CHANGED.equals(event.getTopic())) {
        		unregisterService(adminSession.getNode(path));
        		createService(adminSession.getNode(path));
        	} else if (SlingConstants.TOPIC_RESOURCE_REMOVED.equals(event.getTopic())) {
        		unregisterService(adminSession.getNode(path));
        	}
        } catch (RepositoryException e) {
        	log.error("Cannot handle event ",e);
        }	
	}

	
	private void initialLoad() throws PathNotFoundException, RepositoryException {
		loadNodes (adminSession.getNode(configPath));
	}
	
	private void loadNodes (Node n) throws RepositoryException {
		NodeIterator ni = n.getNodes();
		while (ni.hasNext()) {
			Node n1 = ni.nextNode();
			createService (n1);
			if (n1.hasNodes()) {
				loadNodes (n1);
			}
		}
	}
	
	/**
	 * Try to instantiate a healthStatusProvider on a definition
	 * @param n -- the Node, where the definition is located
	 * @return
	 * @throws RepositoryException 
	 */
	private void createService (Node n) throws RepositoryException {

		MBeanServer server = ManagementFactory.getPlatformMBeanServer();

		if (! n.hasProperty("JMXproperty")) {
			return;
		}

		String mbeanName = n.getName().replace("_", ":");
		ObjectName mbean = null;
		try {
			mbean = new ObjectName (mbeanName);
		} catch (MalformedObjectNameException e) {
			log.error("Cannot create ObjectName",e);
		} catch (NullPointerException e) {
			log.error("Cannot create ObjectName",e);
		}
		if (mbean != null) {
			Set<ObjectName> beans = server.queryNames(mbean, null);
			if (beans.size() == 1) {
				log.info ("Instantiate healtcheck for MBean "+ mbeanName);

				Map<String,Property> props = new HashMap<String,Property>();
				PropertyIterator pi = n.getProperties();
				while (pi.hasNext()) {
					Property p = (Property) pi.next();
					props.put(p.getName(), p);
				}
				MBeanStatusProvider msp = new MBeanStatusProvider (beans.iterator().next(),props);


				Dictionary<String,String> params = new Hashtable<String,String>();
				//params.put(Constants.SERVICE_PID, pid );
				params.put(Constants.SERVICE_DESCRIPTION, "Statusprovider for mbean " + mbeanName );

				ServiceRegistration service = registerService (msp,params);
				registeredServices.put (n.getPath(),service);

			} else {
				log.warn("Cannot find mbean "+ mbeanName + ", found "+ beans.size());
			}
		}

	}
	
    // helper to register services
    
    private ServiceRegistration registerService (MBeanStatusProvider service, Dictionary<String,String> params) {
    	return bundleContext.registerService(HealthStatusProvider.class.getName(), service, params);
    }
	
    public void deactivateAllServices() {
    	Iterator<ServiceRegistration> i = registeredServices.values().iterator();
    	while (i.hasNext()) {
    		ServiceRegistration sr = i.next();
    		sr.unregister();
    	}
    	registeredServices.clear();
    }
	
    /**
     * unregister a service defined by a special node
     * @param definition
     */
    private void unregisterService(Node definition) {
    	ServiceRegistration r = registeredServices.get(definition);
    	if (r != null) {
    		r.unregister();
    		registeredServices.remove(definition);
    	}
    }


	
}
