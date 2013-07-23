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
package de.joerghoh.cq5.jmx.bundles;

import java.util.Dictionary;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * 
 * This class makes some OSGI bundle statistics available tbe Mbean "de.joerghoh.cq5.jmx:id=bundles".
 * 
 * Currently these are:
 * * the number of bundles in active state
 * * the number of bundles in installed state
 * * the number of bundles in resolved state
 * * the number of fragment bundles
 * 
 * Usually you would observe the number of bundles in installed and resolved state and make
 * sure that these numbers are always 0.
 * 
 * Also a blacklist feature is available. Bundles on this blacklist are ignored when
 * counting the numbers listed above. If you need to have bundles not in an active state
 * (best example: org.apache.sling.jcr.webdav due to the CQ5 security checklist), you can put
 * the symbolic name of it to the blacklist. Then you still can check the number of bundles in
 * installed state for being 0.
 * 
 * 
 * @author joerg@joerghoh.de
 *
 */

@Component(immediate = true, metatype = true, description = "Exports some bundle status to JMX", label = "JMX bundle status provider")
@Service(value = BundleStatusMBean.class)
@Properties({ 
	@Property(name = "jmx.objectname", value = "de.joerghoh.cq5.jmx:id=bundles", propertyPrivate=true) 
})
public class BundleStatusImpl implements BundleStatusMBean {
	
	Logger log = LoggerFactory.getLogger(this.getClass());

	@Property(cardinality=Integer.MAX_VALUE, description="The symbolic name of bundles which should be ignored", label="Ignored bundles")
	private final static String PROP_IGNORED_BUNDLES="ignoredBundles"; 
	String[] ignored_bundles;
	
	private BundleContext bctx;

	// TODO implement event listening??

	public int getActiveBundles() {
		Bundle[] bundles = bctx.getBundles();
		int activeBundleCount = 0;
		for (Bundle b : bundles) {
			
			if (isIgnored(b)) {
				continue;
			}

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
		ignored_bundles = PropertiesUtil.toStringArray(ctx.getProperties().get(PROP_IGNORED_BUNDLES));
		if (ignored_bundles != null ) {
			log.info ("Ignoring bundles {}", ignored_bundles.toString());
		}
	
	}

	public int getFragmentBundles() {
		Bundle[] bundles = bctx.getBundles();
		int fragmentBundleCount = 0;
		for (Bundle b : bundles) {
			
			if (isIgnored(b)) {
				continue;
			}

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
			
			if (isIgnored(b)) {
				continue;
			}

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
			
			if (isIgnored(b)) {
					continue;
			}

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
	
	/**
	 * helper method to determine if a certain bundel is 
	 * on the ignored_bundles list.
	 * @param b the bundle to check
	 * @return true if the bundle should be ignored
	 */
	private boolean isIgnored(Bundle b) {
		if (ignored_bundles == null) {
			return false;
		}
		String name = b.getSymbolicName();
		for (int i=0; i< ignored_bundles.length;i++) {
			String ignored = ignored_bundles[i];
			if (ignored.equals(name)) {
				return true;
			}
		}
		
		return false;
	}
	
}
