/*
 * Copyright 2012 Jörg Hoh, Alexander Saar, Markus Haack
 * 
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package de.joerghoh.cq5.healthcheck;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HealthStatus {

	private Logger log = LoggerFactory.getLogger(HealthStatus.class);

	private int status;
	private String reason;
	private String provider;

	public HealthStatus(int status, String reason, String provider) {
		this.status = status;
		this.reason = reason;
		this.provider = provider;
	}

	public int getStatus() {
		return status;
	}

	public String getStatusText() {
		switch (status) {
		case de.joerghoh.cq5.healthcheck.HealthStatusProvider.OK:
			return "OK";
		case de.joerghoh.cq5.healthcheck.HealthStatusProvider.WARN:
			return "WARN";
		case de.joerghoh.cq5.healthcheck.HealthStatusProvider.CRITICAL:
			return "CRITICAL";
		default:
			log.error("Invalid status: " + status);
			return "UNKNOWN";
		}
	}

	public String getMessage() {
		return reason;
	}

	public String getProvider() {
		return this.provider;
	}
}
