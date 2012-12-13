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
package de.joerghoh.cq5.jmx.requests;

import java.util.concurrent.atomic.AtomicLong;

public class RequestInformationImpl implements RequestInformationMBean {

	private AtomicLong counter = new AtomicLong(0);

	private AtomicLong duration = new AtomicLong(0);

	private String mimeType;

	// MBean interface

	public String getMimeType() {
		return mimeType;
	}

	public long getRequestCounter() {
		return counter.get();
	}

	public long getTotalRequestDuration() {
		return duration.get();
	}

	// protected interface

	protected RequestInformationImpl(String mimeType) {
		this.mimeType = mimeType;
	}

	protected void update(long millis) {
		counter.incrementAndGet();
		duration.addAndGet(millis);

	}
}
