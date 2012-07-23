package de.joerghoh.cq5.healthcheck.providers.replication;

import javax.management.MBeanServer;
import javax.management.ObjectName;

public class ReplicationAgentStatusOptions {
	
		
		private MBeanServer mbeanServer;
		private ObjectName objectName;
		private String pid;
		
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
