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

public interface OsgiEventCounterMBean {

	/**
	 * Return the number of OSGI events which have been posted since startup
	 * @return
	 */
	long getTotalEventCounter();
	
	/**
	 * Returns the number of events, which applies to Sling resources (event: org/apache/sling/api/resource/Resource/*)
	 * @return
	 */
	long getResourceEventCounter();
	
	/**
	 * Returns the number of events, which apply to replication jobs (event: com/day/cq/replication/job/publish)
	 * @return
	 */
	long getReplicationEventCounter();

	
	/**
	 * Returns the number of all OSGI relevant events (org/osgiframework/*")
	 * @return
	 */
	long getOsgiEventCounter();
	
}
