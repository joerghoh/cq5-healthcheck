package de.joerghoh.cq5.healthcheck.jmx;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;

// add OSGI exports for all implementing classes
@Service
@Component(immediate=true,metatype=true)
@Properties({
	@Property(name="jmx.objectname",value="sample.jmx:id='requestCounter'"),
	@Property(name="pattern",value="/.*")
})

public class RequestCounter implements RequestCounterMBean, Filter{

	private int count = 0;
	
	public int getRequestCount() {
		// TODO Auto-generated method stub
		return count;
	}

	public void destroy() {
		// nothing to do
		
	}

	public void doFilter(ServletRequest request, ServletResponse response,
			FilterChain chain) throws IOException, ServletException {
		count++;
		chain.doFilter(request, response);
		
	}

	public void init(FilterConfig arg0) throws ServletException {
		// nothing to do
	}

	
	
}
