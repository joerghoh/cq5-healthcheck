
How to integrate the framework
==============

To use the cq5-healthcheck framework in your CQ5 application, you first should add the dependencies to your dependency management section (normally in your parent POM)

```
   <dependencyManagement>
        ...
	<depdencies>
	    ...
            <dependency>
                <groupId>de.joerghoh.cq5.healthcheck</groupId>
                <artifactId>core</artifactId>
                <version>1.0.0</version>
            </dependency>
            <dependency>
                <groupId>de.joerghoh.cq5.healthcheck</groupId>
                <artifactId>jmx-extension</artifactId>
                <version>1.0.0</version>
            </dependency>
            <dependency>
                <groupId>de.joerghoh.cq5.healthcheck</groupId>
                <artifactId>api</artifactId>
                <version>1.0.0</version>
            </dependency>
        </dependencies>
    </dependencyManagement>
``

Just add the jmx-extension artifact to get some more out-of-the-box Mbeans you can monitor. 

In your POM you should reference only the API package:


```
	<dependencies>
		...
		<dependency>
			<groupId>de.joerghoh.cq5.healthcheck</groupId>
			<artifactId>api</artifactId>
		</dependency>
	</dependencies>
``

As the healthcheck framework is not included in the CQ5 standard, you need to install it to your CQ5. The easiest way is to embedd the relevant OSGI bundles to your content packages (do not forget to declare every bundles you want to embedd also as dependencies). You can do it like this.

```
	    <plugin>
                <groupId>com.day.jcr.vault</groupId>
                <artifactId>maven-vault-plugin</artifactId>
                <extensions>true</extensions>
                <configuration>
                    <group>myApp</group>
                    <requiresRoot>true</requiresRoot>

                    <install>true</install>
                    <verbose>true</verbose>

                    <packageFile>${project.build.directory}/${project.artifactId}-${project.version}.zip</packageFile>

                    <version>${project.version}</version>
                    <properties>
                        <acHandling>overwrite</acHandling>
                    </properties>
		    </embeddeds>
                	<embedded>
				<groupId>de.joerghoh.cq5.healthcheck</groupId>
	                        <artifactId>core</artifactId>
				<target>/apps/healthcheck/install</target>
	                </embedded>
			<embedded>
                                <groupId>de.joerghoh.cq5.healthcheck</groupId>
				<artifactId>api</artifactId>
	                        <target>/apps/healthcheck/install</target>
			</embedded>
                        <embedded>
				<groupId>de.joerghoh.cq5.healthcheck</groupId>
				<artifactId>jmx-extension</artifactId>
				<target>/apps/healthcheck/install</target>
	                </embedded>

                    </embeddeds>
                </configuration>
            </plugin>

```

Now you have deployed the framework bundles to your application, and you can start to leverage it.


Checking the healthstatus of CQ
===================

Loadbalancer Servlet
===

The framework provides only a rudimentary method to get the system status; that's the loadbalancer servlet (de.joerghoh.cq5.healthcheck.impl.LoadbalancerServlet); the primary focus of this servlet is to instruct the loadbalancer to forward requests to this CQ instance or not. It does not only use the system healthstatus, but also the role of this CQ instance in a cluster, to determine if the loadbalancer should send requests to this CQ instance.

If the loadbalancer should send requests to this instance, a statuscode 200 and the string "OK" is returned as response body. In any other case, the statuscode 500 and the string "WARN" is returned.


Currently this servlet supports these 2 clustering modes:
* "ActiveActive": The loadbalancer should send requests to all nodes (master & slaves) of a CRX cluster.
* "ActivePassive": The loadbalancer should send requests only to the master node of the CRX cluster.

You can configure this mode in the OSGI properties of the LoadbalancerServlet. 
Please note: This does not change anything on the repository level, but only decides, which node is receiving user requests via the laodbalancer!

If you need a more sophisticated version to direct the loadbalancer, you should implement your own version of this servlet.


The status page
====

The framework does not come with a detailled statuspage, but you can easily build one yourself. A sample is contained in the content package on https://github.com/joerghoh/cq5-healthcheck, which you can use a basis for your own statuspage.
