package de.joerghoh.cq5.healthcheck.impl.providers.jcr;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.EventListener;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
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

@Component (immediate=true, metatype=true,description="JCR Observation Delay checker", label="JCR Observation Delay Checker")
@Service
public class JcrObservationDelayProvider implements EventListener, StatusProvider {

	Logger log = LoggerFactory.getLogger(this.getClass());


	private static final long DEFAULT_DELAY_WARN = 500;
	@Property(longValue=DEFAULT_DELAY_WARN, label="threshold for WARN", description="Threshold to reach WARN level in miliseconds")
	private static final String DELAY_WARN = "delay.warn";
	private long delayWarn;
	
	private static final long DEFAULT_DELAY_CRITICAL = 2000;
	@Property(longValue=DEFAULT_DELAY_CRITICAL, label="threshold for CRITICAL", description="Threshold to reach CRITICAL level in miliseconds")
	private static final String DELAY_CRITICAL = "delay.critical";
	private long delayCritical;
	
	private static final long DEFAULT_SCHEDULING_INTERVAL = 60; // seconds
	@Property(longValue=DEFAULT_SCHEDULING_INTERVAL, label="Scheduling interval", description="Interval between checks in seconds")
	private static final String SCHEDULING_INTERVAL = "scheduling.interval";
	private long schedulingInterval;
	
	
	private static final String JOBNAME = "de.joerghoh.cq5.healthcheck.impl.providers.jcr.jcrObservationDelayProvider";
	private static final String tempDir = "/var/JcrObservationDelay";

	private Session listenSession = null;
	
	private long lastDelay = 0;

	@Reference
	Scheduler scheduler;
	
	@Reference
	SlingRepository repo;


	@Activate
	protected void activate (ComponentContext ctx) {
		Session readWriteSession = null;
		delayWarn = PropertiesUtil.toLong(ctx.getProperties().get(DELAY_WARN), DEFAULT_DELAY_WARN);
		delayCritical = PropertiesUtil.toLong(ctx.getProperties().get(DELAY_CRITICAL), DEFAULT_DELAY_CRITICAL);
		schedulingInterval = PropertiesUtil.toLong (ctx.getProperties().get(SCHEDULING_INTERVAL), DEFAULT_SCHEDULING_INTERVAL);
		
		try {
			readWriteSession = repo.loginAdministrative(null);
			if (!readWriteSession.getRootNode().hasNode(tempDir.replaceFirst("/", ""))) {
				log.info("Created temp directory");
				JcrUtil.createPath(tempDir, "nt:unstructured", readWriteSession);
				readWriteSession.save();
			}

			//Node tmpNode = readWriteSession.getNode(tempDir);
			//Long now = System.currentTimeMillis();
			//NodeIterator childs = tmpNode.getNodes();
		
		} catch (RepositoryException e) {
			log.error ("Cannot create Session for setting up temp directory:",e);
		} finally {
			if (readWriteSession != null && readWriteSession.isLive()) {
				readWriteSession.logout();
			}
		}
		try {
			listenSession = repo.loginAdministrative(null);
			startListen();
		} catch (RepositoryException e) {
			log.error ("Cannot create listenSession",e);
		}
		try {
			scheduler.fireJob(new ObservationTrigger(tempDir),null);
			scheduler.addPeriodicJob(JOBNAME, new ObservationTrigger(tempDir), null, schedulingInterval, false);
		} catch (Exception e) {
			log.error("Cannot start scheduler",e);
		}
		log.info("JcrObservationDelay checker started");
		
	}



	@Deactivate
	protected void deactivate (ComponentContext ctx) {

		try {
			stopListen();
		} catch (RepositoryException e) {
			log.error ("Cannot stop observation listener",e);
		}
		if (listenSession != null && listenSession.isLive()) {
			listenSession.logout();
		}
		scheduler.removeJob(JOBNAME);
		log.info("JcrObservationDelay checker stopped");
	}
	
	private void stopListen() throws UnsupportedRepositoryOperationException, RepositoryException {
		listenSession.getWorkspace().getObservationManager().removeEventListener(this);
		
	}

	private void startListen() throws UnsupportedRepositoryOperationException, RepositoryException {
		listenSession.getWorkspace().getObservationManager().addEventListener(
				this, Event.NODE_ADDED, tempDir, true, null, null, true);
		
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
				long timestamp = foundNode.getProperty("timestamp").getLong();
				lastDelay = now - timestamp;
				log.debug("Current observation delay: {} ms", lastDelay);
				removeNode (foundNode.getPath());
			}
		}
		}catch (RepositoryException e) {
			log.error ("Cannot handle event",e);
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
	
	
	@Override
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
	 * runnable implementation, which create a change to the repository
	 */

	private class ObservationTrigger implements Runnable {

		private String rootPath;
		Logger log = LoggerFactory.getLogger(this.getClass());

		protected ObservationTrigger (String rootNode) {
			this.rootPath = rootNode;
		}



		@Override
		public void run() {
			createNewNode();

		}

		private void createNewNode () {
			Session writeSession = null;
			try {
				writeSession = repo.loginAdministrative(null);
				long now = System.currentTimeMillis();
				String nodeName = "" + now;
				Node rootNode = writeSession.getNode(rootPath);
				Node childNode = rootNode.addNode(nodeName, "nt:unstructured");;
				childNode.setProperty("timestamp",now);
				writeSession.save();
				
			} catch (RepositoryException e) {
				log.warn ("Cannot create timestamp node",e);
			} finally {
				if (writeSession != null && writeSession.isLive()) {
					writeSession.logout();
				}
			}
			
		}

	}




}
