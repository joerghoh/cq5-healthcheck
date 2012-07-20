package de.joerghoh.cq5.healthcheck.providers;

import java.lang.management.ManagementFactory;

import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.ReflectionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.joerghoh.cq5.healthcheck.HealthStatus;
import de.joerghoh.cq5.healthcheck.HealthStatusProvider;

/**
 * Abstract class which provides support for access mbeans
 * @author joerg
 *
 */
public abstract class AbstractMBeanProvider implements HealthStatusProvider {
    
    private Logger log = LoggerFactory.getLogger(AbstractMBeanProvider.class);

    public abstract HealthStatus getHealthStatus();

    
    protected String getMBeanStringValue (String mbeanName, String property) {
        String value = null;
        
        Object r = getValue (mbeanName, property);
        if (r != null) {
            value = r.toString();
        } 
        return value;
    }
    
    protected int getMBeanIntValue (String mbeanName, String property) {
        int value = 0;
        
        Object r = getValue (mbeanName, property);
        if (r != null) {
            value = ((Integer) r).intValue();
        } 
        return value;
    }
    
    protected long getMBeanLongValue (String mbeanName, String property) {
        long value = 0;
        
        Object r = getValue (mbeanName, property);
        if (r != null) {
            value = ((Long) r).longValue();
        } 
        return value;
    }
    
    private Object getValue (String mbeanName, String property) {
        MBeanServer server = ManagementFactory.getPlatformMBeanServer();
        
        ObjectName name = null;
        Object result = null;
        try {
            name = new ObjectName (mbeanName);
            result = server.getAttribute(name,property);
        } catch (MalformedObjectNameException e) {
            log.error ("Cannot get mbean " + mbeanName, e);
        } catch (NullPointerException e) {
            log.error ("Cannot get mbean " + mbeanName, e);
        } catch (AttributeNotFoundException e) {
            log.error ("Cannot get mbean " + mbeanName, e);
        } catch (InstanceNotFoundException e) {
            log.error ("Cannot get mbean " + mbeanName, e);
        } catch (MBeanException e) {
            log.error ("Cannot get mbean " + mbeanName, e);
        } catch (ReflectionException e) {
            log.error ("Cannot get mbean " + mbeanName, e);
        }
        return result;
    }
    
    
    
}
