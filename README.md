cq5-healthcheck
===============

This small project supports you when you need to monitor your CQ5 system. "Monitoring" means that an 
automatic process checks CQ5 every few seconds (or minutes) if it is still fully functional. By default
CQ5 does not have an endpoint, which offers that kind of information, so I created this project to provide them. 
This information can be consumed by both a automatic monitoring system (e.g. Nagios), but is also a big help for any person, which is supposed to run CQ5 instances.

The projects consists of these elements:
* A central HealthStatusService, which collects the status information of all registered StatusProviders
* A MBeanStatusProvider factory, which allows to monitor MBeans
* A CQ page component, which can display the current state fetched from the HealthStatusService

You can implement arbitrary HealthStatusProviders to report the status of any module or subsystem, for example:

* Connectivity to external systems is ok
* The average response time to user requests is below a certain threshold
* The error rate of user requests is below 0.001%
* ...

Currently only a small number of HealthStatusProviders is available.  In the bundle JMX extensions also a number of custom MBeans are contained; these can be used by the MBeanStatusProvider.


This project is released under Apache License.


Quick start
============

* Build the package using "mvn package", you'll find a CQ5 package in apps/target/ 
* Install it in CQ5
* Open the /content/statuspage.html page to find an example of a health status page.


Example config
=============

Most functionality of this project lies in the ability to easily query MBeans and provide
thresholds for the 3 states "OK", "Warning" and "Critical".

These configurations are stored in the repository and be created at any time. Whenever a
configuration is changed, these changes are picked up and applied immediately.

So, for a first, let's configure some thresholds for the "publish1" replication agent. We want,
that whenever the length of the queue reaches 100 or higher, that the monitoring reports a 
warning. Use CRXDE (Lite) for this:

* Create a folder structure /apps/healthConfig/config. You can create any
  appropriate structure to hold OSGI config data (including combining with
  runmodes, etc...)
* Create a nt:file node with the name "de.joerghoh.cq5.healthcheck.impl.providers.MBeanStatusProvider-publish1_queue_warn.config
* Add a line 'mbean.name="com.adobe.granite.replication:type\=agent,id\=\"publish1\""' (without the single quotes)
* Add a line 'mbean.property="QueueNumEntries.warn.>.100"' (without the single quotes)
* Add a line 'mbean.providerHint="Warn on queue length"' (without the single quotes)
* Save

Note: 
* You can also use a node type of "sling:OsgiConfig" and add each line as a separate property; the part before the equal sign "=" will be the property name, 
  the remaining part the value. 
* In the method described here you need to escape the " (double quote) and the ´=´ (equal sign) by a backslash (´\´)

When you reload your statuspage, you should see an additional entry with the mbean name "com.adobe.granite.replication:type=agent,id="publish1""
and status "OK", if the queue size of this replication agent is smaller than 100.


How to configure (monitor MBeans)
=============

You've seen, that we created a new node containing the check definition. The
node name denotes the ServiceFactory
("de.joerghoh.cq5.healthcheck.impl.providers.MBeanStatusProvider") and a part
which you can choose freely. Choose a speaking name for it.

The name of the mbean is given in the assignment to the property  "mbean.name", while the monitoring condition is encoded in the assignment of the property
 "mbean.property".


The definition attribute consists of 4 elements:
* The name of the MBean attribute, we want to monitor
* The level we want to apply this definition for (allowed values: "ok", "warn", "critical").
* The comparator function we want to use.
* the comparator value we want to compare the actual value of that mbean attribute to.

Currently the following comparator functions are supported:
* "<" (smaller than, supported for long and integer values)
* ">" (larger than, supported for long and integer values)
* "=" (equals for number comparison, supported for long and integer values)
* "!=" (not equal for number comparison, supported for long and integer values)
* "equals" (equals for string comparison)
* "notequals" (non-equality for string comparison)

The statuspage
================

By default this project ships with a statuspage in /content/statuspage.html; it displays

* an overall status
* individual status information for each available mbean

The overall status is composed the individual status informations like this:
* If at least one mbean returns "WARN", and no mbean returns "CRITICAL", the overall status is "WARN".
* If at least one mbean returns "CRITICAL", the overall status is "CRITICAL".
* If all mbeans return "OK", the overall status is "OK".

There is an additional condition:

* If there are less mbeans available than configured in the property "bundle.threshold" (service "HealthCheck service"),
  the overall status is "CRITICAL".
  
This reflects a situation, where bundles are not loaded and therefor mbeans and their checks are not available. 
In most cases this occurs during startup time and will go away, when all bundles activated and their services are running.
  



