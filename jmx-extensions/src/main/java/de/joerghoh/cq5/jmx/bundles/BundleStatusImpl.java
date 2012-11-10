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
package de.joerghoh.cq5.jmx.bundles;

import java.util.Dictionary;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;

@Component(immediate = true, metatype = true, description = "Exports some bundle status to JMX", name = "JMX bundle status provider")
@Service(value = BundleStatusMBean.class)
@Properties({ @Property(name = "jmx.objectname", value = "de.joerghoh.cq5.jmx:id=bundles") })
public class BundleStatusImpl implements BundleStatusMBean {

	private BundleContext bctx;

	// TODO implement event listening??

	public int getActiveBundles() {
		Bundle[] bundles = bctx.getBundles();
		int activeBundleCount = 0;
		for (Bundle b : bundles) {

			@SuppressWarnings("unchecked")
			Dictionary<String, String> headers = b.getHeaders();
			boolean isFragment = (headers.get("Fragment-Host") != null);
			boolean isActive = (b.getState() == Bundle.ACTIVE);

			if (!isFragment && isActive) {
				activeBundleCount++;
			}
		}
		return activeBundleCount;
	}

	@Activate
	protected void activate(ComponentContext ctx) {
		bctx = ctx.getBundleContext();
	}

	public int getFragmentBundles() {
		Bundle[] bundles = bctx.getBundles();
		int fragmentBundleCount = 0;
		for (Bundle b : bundles) {

			@SuppressWarnings("unchecked")
			Dictionary<String, String> headers = b.getHeaders();
			boolean isFragment = (headers.get("Fragment-Host") != null);

			if (isFragment) {
				fragmentBundleCount++;
			}
		}
		return fragmentBundleCount;
	}

	public int getInstalledBundles() {
		Bundle[] bundles = bctx.getBundles();
		int installedBundleCount = 0;
		for (Bundle b : bundles) {

			boolean isInstalled = (b.getState() == Bundle.INSTALLED);

			if (isInstalled) {
				installedBundleCount++;
			}
		}
		return installedBundleCount;
	}

	public int getResolvedBundles() {
		Bundle[] bundles = bctx.getBundles();
		int resolvedBundleCount = 0;
		for (Bundle b : bundles) {

			@SuppressWarnings("unchecked")
			Dictionary<String, String> headers = b.getHeaders();
			boolean isResolved = (b.getState() == Bundle.RESOLVED);
			boolean isFragment = (headers.get("Fragment-Host") != null);

			if (isResolved && !isFragment) {
				resolvedBundleCount++;
			}
		}
		return resolvedBundleCount;
	}
}
