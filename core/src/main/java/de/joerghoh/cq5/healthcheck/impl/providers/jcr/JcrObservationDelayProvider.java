/*
 * Copyright 2012 Jörg Hoh, Alexander Saar, Markus Haack
 * 
 *   Licensed under the Apache License, Version 2.0 (the "License");
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
package de.joerghoh.cq5.healthcheck.impl.providers.jcr;

import java.util.Dictionary;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.EventListener;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.commons.scheduler.Scheduler;
import org.apache.sling.jcr.api.SlingRepository;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.day.cq.commons.jcr.JcrUtil;

import de.joerghoh.cq5.healthcheck.Status;
import de.joerghoh.cq5.healthcheck.StatusCode;
import de.joerghoh.cq5.healthcheck.StatusProvider;

/**
 * JCR Observation Delay Provider
 * 
 * @author joerg@joerghoh.de (Jörg Hoh)
 * 
 *         This statusprovider delivers information about the current delay in
 *         the JCR observation mechanism. For this reason it creates nodes and
 *         measures the time the creation of the node and the time, when the
 *         creation event is finally delivered to this provider. This time isn't
 *         really accurate, as it also includes the saving of the session (which
 *         shouldn't be a really big problem, since the node is really small),
 *         but in any way it can give you indications about any delays causing
 *         problems with the processing of both JCR observation and Sling
 *         events.
 * 
 *         A clear implementation should be placed into the JCR repository,
 *         which should annotate each event with a timestamp and measure the
 *         delay there.
 */
@Component(immediate = true, metatype = true, description = "JCR Observation Delay checker", label = "JCR Observation Delay Checker", policy = ConfigurationPolicy.REQUIRE)
@Service
public class JcrObservationDelayProvider implements EventListener,
		StatusProvider {

	private Logger log = LoggerFactory.getLogger(this.getClass());

	@Property
	private static String CATEGORY = "provider.category";
	private String category;
	
	private static final long DEFAULT_DELAY_WARN = 500;
	@Property(longValue = DEFAULT_DELAY_WARN, label = "threshold for WARN", description = "Threshold to reach WARN level in miliseconds")
	private static final String DELAY_WARN = "delay.warn";
	private long delayWarn;

	private static final long DEFAULT_DELAY_CRITICAL = 2000;
	@Property(longValue = DEFAULT_DELAY_CRITICAL, label = "threshold for CRITICAL", description = "Threshold to reach CRITICAL level in miliseconds")
	private static final String DELAY_CRITICAL = "delay.critical";
	private long delayCritical;

	private static final long DEFAULT_SCHEDULING_INTERVAL = 60; // seconds
	@Property(longValue = DEFAULT_SCHEDULING_INTERVAL, label = "Scheduling interval", description = "Interval between checks in seconds")
	private static final String SCHEDULING_INTERVAL = "scheduling.interval";
	private long schedulingInterval;

	private static final String JOBNAME = "de.joerghoh.cq5.healthcheck.impl.providers.jcr.jcrObservationDelayProvider";
	private static final String tempDir = "/var/JcrObservationDelay";

	private Session listenSession = null;

	private long lastDelay = 0;

	@Reference
	private Scheduler scheduler;

	@Reference
	private SlingRepository repo;

	@Activate
	protected void activate(ComponentContext ctx) {
		Session readWriteSession = null;
		
		Dictionary<?, ?> props = ctx.getProperties();
		delayWarn = PropertiesUtil.toLong(props.get(DELAY_WARN), DEFAULT_DELAY_WARN);
		delayCritical = PropertiesUtil.toLong(props.get(DELAY_CRITICAL), DEFAULT_DELAY_CRITICAL);
		schedulingInterval = PropertiesUtil.toLong(props.get(SCHEDULING_INTERVAL), DEFAULT_SCHEDULING_INTERVAL);
		category = PropertiesUtil.toString(props.get(CATEGORY), null);

		try {
			readWriteSession = repo.loginAdministrative(null);
			if (!readWriteSession.getRootNode().hasNode(
					tempDir.replaceFirst("/", ""))) {
				log.info("Created temp directory");
				JcrUtil.createPath(tempDir, "nt:unstructured", readWriteSession);
				readWriteSession.save();
			}

			// Node tmpNode = readWriteSession.getNode(tempDir);
			// Long now = System.currentTimeMillis();
			// NodeIterator childs = tmpNode.getNodes();

		} catch (RepositoryException e) {
			log.error("Cannot create Session for setting up temp directory:", e);
		} finally {
			if (readWriteSession != null && readWriteSession.isLive()) {
				readWriteSession.logout();
			}
		}
		try {
			listenSession = repo.loginAdministrative(null);
			startListen();
		} catch (RepositoryException e) {
			log.error("Cannot create listenSession", e);
		}
		try {
			scheduler.fireJob(new ObservationTrigger(tempDir), null);
			scheduler.addPeriodicJob(JOBNAME, new ObservationTrigger(tempDir),
					null, schedulingInterval, false);
		} catch (Exception e) {
			log.error("Cannot start scheduler", e);
		}
		log.info("JcrObservationDelay checker started");

	}

	@Deactivate
	protected void deactivate(ComponentContext ctx) {

		try {
			stopListen();
		} catch (RepositoryException e) {
			log.error("Cannot stop observation listener", e);
		}
		if (listenSession != null && listenSession.isLive()) {
			listenSession.logout();
		}
		scheduler.removeJob(JOBNAME);
		log.info("JcrObservationDelay checker stopped");
	}

	private void stopListen() throws UnsupportedRepositoryOperationException,
			RepositoryException {
		listenSession.getWorkspace().getObservationManager()
				.removeEventListener(this);

	}

	private void startListen() throws UnsupportedRepositoryOperationException,
			RepositoryException {
		listenSession
				.getWorkspace()
				.getObservationManager()
				.addEventListener(this, Event.NODE_ADDED, tempDir, true, null,
						null, true);

	}

	/**
	 * Do the event handling, calculate delay and drop the node afterwards.
	 */
	@Override
	public void onEvent(EventIterator events) {
		try {
			while (events.hasNext()) {
				Event e = events.nextEvent();
				if (e.getPath().startsWith(tempDir)) {
					Node foundNode = listenSession.getNode(e.getPath());
					long now = System.currentTimeMillis();
					long timestamp = foundNode.getProperty("timestamp")
							.getLong();
					lastDelay = now - timestamp;
					log.debug("Current observation delay: {} ms", lastDelay);
					removeNode(foundNode.getPath());
				}
			}
		} catch (RepositoryException e) {
			log.error("Cannot handle event", e);
		}
	}

	private void removeNode(String foundNode) throws RepositoryException {
		Session writeSession = null;
		try {
			writeSession = repo.loginAdministrative(null);
			writeSession.removeItem(foundNode);
			writeSession.save();

		} finally {
			if (writeSession != null && writeSession.isLive()) {
				writeSession.logout();
			}
		}
	}

	/**
	 * @see de.joerghoh.cq5.healthcheck.StatusProvider#getStatus()
	 */
	public Status getStatus() {
		StatusCode sc = StatusCode.OK;
		if (lastDelay > delayCritical) {
			sc = StatusCode.CRITICAL;
		} else if (lastDelay > delayWarn) {
			sc = StatusCode.WARN;
		}
		String message = "Delay " + lastDelay + " ms";
		String provider = "JcrObservationDelay";

		Status status = new Status(sc, message, provider);
		return status;
	}
	
	/**
	 * @see de.joerghoh.cq5.healthcheck.StatusProvider#getCategory()
	 */
	public String getCategory() {
		return category != null ? category : DEFAULT_CATEGORY;
	}

	/**
	 * runnable implementation, which create a change to the repository
	 */
	private class ObservationTrigger implements Runnable {

		private String rootPath;
		private Logger log = LoggerFactory.getLogger(this.getClass());

		protected ObservationTrigger(String rootNode) {
			this.rootPath = rootNode;
		}

		public void run() {
			createNewNode();
		}

		private void createNewNode() {
			Session writeSession = null;
			try {
				writeSession = repo.loginAdministrative(null);
				long now = System.currentTimeMillis();
				String nodeName = "" + now;
				Node rootNode = writeSession.getNode(rootPath);
				Node childNode = rootNode.addNode(nodeName, "nt:unstructured");
				;
				childNode.setProperty("timestamp", now);
				writeSession.save();

			} catch (RepositoryException e) {
				log.warn("Cannot create timestamp node", e);
			} finally {
				if (writeSession != null && writeSession.isLive()) {
					writeSession.logout();
				}
			}
		}
	}
}
