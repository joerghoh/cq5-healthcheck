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
package de.joerghoh.cq5.healthcheck.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.List;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.joerghoh.cq5.healthcheck.Status;
import de.joerghoh.cq5.healthcheck.StatusCode;
import de.joerghoh.cq5.healthcheck.StatusProvider;
import de.joerghoh.cq5.healthcheck.StatusService;

@Component(metatype = true, immediate = true, label = "HealthCheck Service", description = "Core of the healthcheck, computes the overall result")
@Service(value = StatusService.class)
@Reference(name = "healthStatusProvider", referenceInterface = StatusProvider.class, cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE, policy = ReferencePolicy.DYNAMIC)
public class HealthStatusServiceImpl implements StatusService {

	private List<StatusProvider> providers = new ArrayList<StatusProvider>();
	private final Logger log = LoggerFactory
			.getLogger(HealthStatusServiceImpl.class);

	private static final int DEFAULT_NUMBER_BUNDLES = 0;

	@Property(intValue = DEFAULT_NUMBER_BUNDLES, label = "Number of healthcheck providers", description = "If the number of active healthchecks is not identical to this number you'll get warnings")
	private static String BUNDLE_NUMBER_THRESHOLD_PROP = "bundle.threshold";
	private int bundleNumberThreshold;

	/**
	 * Get overall status
	 */
	public Status getStatus() {
		return getStatus(new String[0]);
	}

	/**
	 * Get status for categories
	 */
	public Status getStatus(String[] categories) {
		return getStatus(categories, bundleNumberThreshold);
	}
	
	/**
	 * Get status for categories
	 */
	public Status getStatus(String[] categories, int bundleNumberThreshold) {
		StatusCode finalStatus = StatusCode.OK;
		List<Status> results = new ArrayList<Status>();
		String message = "";

		List<String> cats = categories != null ? Arrays.asList(categories)
				: new ArrayList<String>();
		for (StatusProvider p : providers) {
			if (cats.isEmpty() || cats.contains(p.getCategory())) {
				Status s = p.getStatus();
				if (s.getStatus().compareTo(finalStatus) > 0) {
					finalStatus = s.getStatus();
				}
				results.add(s);
			}
		}

		// if not all requested services are available, go critical!
		if (results.size() < bundleNumberThreshold) {
			finalStatus = StatusCode.CRITICAL;
			message = "Only " + results.size() + " out of configured "
					+ bundleNumberThreshold + " monitoring services available";
			log.warn(message);
		}
		log.info("Processed " + results.size() + " providers");
		return new Status(finalStatus, message, results);
	}

	/** helper routines **/

	protected void bindHealthStatusProvider(StatusProvider provider) {
		providers.add(provider);
	}

	protected void unbindHealthStatusProvider(StatusProvider provider) {
		providers.remove(provider);
	}

	@Activate
	protected void activate(ComponentContext context) {
		Dictionary<?, ?> properties = context.getProperties();
		bundleNumberThreshold = PropertiesUtil.toInteger(
				properties.get(BUNDLE_NUMBER_THRESHOLD_PROP),
				DEFAULT_NUMBER_BUNDLES);
	}
}
