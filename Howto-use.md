
How to use
==============

To use the cq5-healthcheck framework in your CQ5 application, add the dependencies to your dependency management section (normally in your parent POM)

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

