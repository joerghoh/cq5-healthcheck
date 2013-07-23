/*
 * Copyright 2012 Jörg Hoh, Alexander Saar, Markus Haack
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
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
		label="OSGI Event Counter MBean",
		description="Exposes details on the number of OSGI events via JMX",
		name="de.joerghoh.cq5.jmx.osgi.OsgiEventCounter"
		)
@Service (value={OsgiEventCounterMBean.class,EventHandler.class})
@Properties({
	@Property (name="jmx.objectname",value="de.joerghoh.cq5.jmx.osgi:id=OsgiEventCounter", 
				label="Name of the MBean",
				description="",
				propertyPrivate=true
				),
	@Property (name="event.topics", value={"org/apache/sling/*","com/day/*","org/osgi/framework/*"}, 
				label="Event topics", 
				description="The event topics which should be counted (wildcards allowed)")
})
public class OsgiEventCounter implements OsgiEventCounterMBean, EventHandler {

	long totalCounter = 0;

	// Resource Events
	// TODO make events configurable??

	private static String SLING_RESOURCE_EVENTS = "org/apache/sling/api/resource/Resource";
	long resourceCounter = 0;

	// Replication Events
	private static String REPLICATION_EVENTS = "com/day/cq/replication/job/publish/";
	long replicationEventCounter = 0;

	// OSGI related events (bundles, components, services)
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
