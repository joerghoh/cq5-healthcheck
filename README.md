cq5-healthcheck
===============

This small project supports you when you need to monitor your CQ5 system. "Monitoring" means that an 
automatic process checks CQ5 every few seconds (or minutes) if it is still fully functional. By default
CQ5 does not have an endpoint, which offers this information, so I created this project to provide them.


Quick start
============

* Build the package using "mvn package", you'll find a CQ5 package in /apps/target/ 
* Install it in CQ5
* Create a new page based on the template /apps/healthcheck/templates/HealthCheck
* Open this page, and you'll get a simple HTML output describing some overall status, but 
that's probably not enough.


Example config
=============

Most functionality of this project lies in the ability to easily query MBeans and provide
thresholds for the 3 states "OK", "Warning" and "Critical".

These configurations are stored in the repository and be created at any time. Whenever a
configuration is changed, these changes are picked up and applied immediately.

So, for a first, let's configure some thresholds for the "publish" replication agent. We want,
that whenever the length of the queue reaches 100 or higher, that the monitoring reports a 
warning. Use CRXDE (Lite) for this:


* Make sure, that a structure /etc/healthcheck/mbeans exists (you can build it using nt:unstructured
or sling:Folder nodes).
* Create a new node /etc/healthcheck/mbeans/replication using a sling:Folder
* Create a new node /etc/healthcheck/mbeans/replication/com.adobe.granite.replication_type=agent,id="flush"
** Add a string property "enabled" and set it to "true"
** Add a string property "QueueNumEntries.warn.>" and set it to "100"
* Save

When you reload your statuspage, you should see an additional entry with the mbean name "com.adobe.granite.replication:type=agent,id="publish""
and status "OK", if the queue size of this replication agent is smaller than 100.


How to configure
=============

You've seen, that we created a new node containing the check definition.

The node names are identical to the name of the mbean, besides that ":" is replaced by "_". The reason
for this is, that ":" has a special meaning in JCR (namespace delimiter), and as we don't want to create 
namespaces, we simple escape it here by "_".

Every definition must have a string property "enabled" set to "true"; if that's not the case, the definition is not
picked up.

The definition attribute consists of 3 elements:
* The name of the MBean attribute, we want to monitor
* The level we want to apply this definition for (allowed values: "ok", "warn", "critical").
* The comparator function we want to use.

Currently the following comparator functions are supported:
* "<" (smaller than)
* ">" (larger than)
* "=" (equals for number comparison)
* "equals" (equals for string comparison)
* "notequals" (non-equality for string comparison)

So if we want to monitor a hypothetical MBean called "foo.bar.bz:type=abc", that the value of the attribute "level" is always set
to "zoo" and have a status "critical" if that's not the case, we would do it like this:

* Create a node /etc/healthcheck/mbeans/foo.bar.bz_type=abc
* Set a string property "enabled" to "true"
* Set a string property "level.critical.notequals" to "zoo".
* Save







