package de.joerghoh.cq5.jmx.osgi;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;



@Component(
		immediate=true,
		metatype=true,
		description="Exposes details on the number of OSGI events",
		name="de.joerghoh.cq5.jmx.osgi.eventCounter"
		)
@Service (value={OsgiCounterMBean.class,EventHandler.class})
@Properties({
	@Property (name="jmx.objectname",value="de.joerghoh.cq5.jmx.osgi:id=OsgiCounter"),
	@Property (name="event.topics", value={"org/apache/sling/*","com/day/*","org/osgi/framework/*"})
})
public class OsgiCounter implements OsgiCounterMBean, EventHandler {

	
	long totalCounter = 0;
	
	// TODO make events configurable??
	
	private static String SLING_RESOURCE_EVENTS = "org/apache/sling/api/resource/Resource";
	long resourceCounter = 0;
	
	private static String REPLICATION_EVENTS = "com/day/cq/replication/job/publish/";
	long replicationEventCounter = 0;
	
	private static String OSGI_EVENTS = "org/osgi/framework/";
	long osgiEventCounter = 0;
	
	
	public void handleEvent(Event event) {
		totalCounter++;
		String topic = event.getTopic();
		if (topic.startsWith(SLING_RESOURCE_EVENTS)) {
			resourceCounter++;
		}
		if (topic.startsWith(REPLICATION_EVENTS)) {
			replicationEventCounter++;
		}
		if (topic.startsWith(OSGI_EVENTS)) {
			osgiEventCounter++;
		}
		
	}
	
	public long getTotalEventCounter() {
		return totalCounter;
	}


	public long getResourceEventCounter() {
		return resourceCounter;
	}

	public long getReplicationEventCounter() {
		return replicationEventCounter;
	}

	public long getOsgiEventCounter() {
		return osgiEventCounter;
	}

}
