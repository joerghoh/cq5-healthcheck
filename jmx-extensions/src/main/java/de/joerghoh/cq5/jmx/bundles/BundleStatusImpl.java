package de.joerghoh.cq5.jmx.bundles;

import java.util.Dictionary;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;


@Component(
		immediate=true,
		metatype=true,
		description="Exports some bundle status to JMX",
		name="JMX bundle status provider"
		)
@Service (value=BundleStatusMBean.class)
@Property (name="jmx.objectname",value="de.joerghoh.cq5.jmx:id=bundles")
public class BundleStatusImpl implements BundleStatusMBean {
	
	private BundleContext bctx;

	public int getActiveBundles() {
		Bundle[] bundles = bctx.getBundles();
		int activeBundleCount = 0;
		for (Bundle b: bundles) {

			Dictionary<String,String> headers = b.getHeaders();
			boolean isFragment = (headers.get("Fragment-Host") != null);
			boolean isActive = (b.getState() == Bundle.ACTIVE);

			if (!isFragment && isActive) {
				activeBundleCount++;
			}
		}
		return activeBundleCount;
	}


	@Activate
	protected void activate (ComponentContext ctx) {
		bctx = ctx.getBundleContext();
	}


	public int getFragmentBundles() {
		Bundle[] bundles = bctx.getBundles();
		int fragmentBundleCount = 0;
		for (Bundle b: bundles) {

			Dictionary<String,String> headers = b.getHeaders();
			boolean isFragment = (headers.get("Fragment-Host") != null);

			if (isFragment) {
				fragmentBundleCount++;
			}
		}
		return fragmentBundleCount;
	}

}
