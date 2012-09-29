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
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.sling.api.SlingConstants;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.resource.ValueMap;
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
	
	@Reference
	ResourceResolverFactory rrfac;
	ResourceResolver adminResolver;
	
	
	private Session adminSession;
	
	@Activate
	protected void activate(ComponentContext ctx) throws RepositoryException {
		try {
			bundleContext = ctx.getBundleContext();
			adminSession = repo.loginAdministrative(null);
			adminResolver = rrfac.getAdministrativeResourceResolver(null);
			initialLoad();
		} catch (RepositoryException e) {
			log.error ("Cannot login to repository",e);
			throw new RepositoryException (e);
		} catch (LoginException e) {
			log.error ("Cannot login to repository",e);
		}
	}
	
	@Deactivate
	protected void deactivate() {
		if (adminSession != null) {
			adminSession.logout();
		}
		if (adminResolver != null) {
			adminResolver.close();
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
        	Resource r = adminResolver.getResource(path);
        	if (SlingConstants.TOPIC_RESOURCE_ADDED.equals(event.getTopic())) {
        		createService(r);
        	} else if (SlingConstants.TOPIC_RESOURCE_CHANGED.equals(event.getTopic())) {
        		unregisterService(r);
        		createService(r);
        	} else if (SlingConstants.TOPIC_RESOURCE_REMOVED.equals(event.getTopic())) {
        		unregisterService(r);
        	}
        } catch (RepositoryException e) {
        	log.error("Cannot handle event ",e);
        }	
	}

	
	private void initialLoad() throws PathNotFoundException, RepositoryException {
		loadNodes (adminResolver.getResource(configPath));
	}
	
	private void loadNodes (Resource res) throws RepositoryException {
		
		createService (res);
		Iterator<Resource> children = res.listChildren();
		while (children.hasNext()) {
			Resource r = children.next();
			loadNodes (r);
		}
	}
	
	/**
	 * Try to instantiate a healthStatusProvider on a definition
	 * @param n -- the Node, where the definition is located
	 * @return
	 * @throws RepositoryException 
	 */
	private void createService (Resource resource) throws RepositoryException {

		MBeanServer server = ManagementFactory.getPlatformMBeanServer();

		ValueMap props = ResourceUtil.getValueMap(resource);
		if (!props.containsKey("JMXproperty")) {
			return;
		}
				

		String mbeanName = resource.getName().replace("_", ":");
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

				MBeanStatusProvider msp = new MBeanStatusProvider (beans.iterator().next(),props);


				Dictionary<String,String> params = new Hashtable<String,String>();
				//params.put(Constants.SERVICE_PID, pid );
				params.put(Constants.SERVICE_DESCRIPTION, "Statusprovider for mbean " + mbeanName );

				ServiceRegistration service = registerService (msp,params);
				registeredServices.put (resource.getPath(),service);

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
    private void unregisterService(Resource resource) {
    	ServiceRegistration r = registeredServices.get(resource.getPath());
    	if (r != null) {
    		r.unregister();
    		registeredServices.remove(resource.getPath());
    	}
    }


	
}
