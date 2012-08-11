package de.joerghoh.cq5.healthcheck.providers.replication;

import java.util.Dictionary;

import javax.management.MBeanServer;
import javax.management.ObjectName;

public class ReplicationAgentStatusOptions {
	
		
		private MBeanServer mbeanServer;
		private ObjectName objectName;
		private String pid;
		private Dictionary properties;
		
		public Dictionary getProperties() {
			return properties;
		}
		public void setProperties(Dictionary properties) {
			this.properties = properties;
		}
		public MBeanServer getMbeanServer() {
			return mbeanServer;
		}
		public void setMbeanServer(MBeanServer mbeanServer) {
			this.mbeanServer = mbeanServer;
		}
		public ObjectName getObjectName() {
			return objectName;
		}
		public void setObjectName(ObjectName objectName) {
			this.objectName = objectName;
		}
		public String getPid() {
			return pid;
		}
		public void setPid(String pid) {
			this.pid = pid;
		}
		
		


}
