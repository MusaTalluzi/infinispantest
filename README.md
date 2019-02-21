# An example using Remot Access to Cache data into an infinispan server via REST

## System requirements
- Java 8.0^ (Java SDK 1.8)
- Maven 3.0^
- [Infinispan Server](http://infinispan.org/download/).

## Configure maven to download artifacts from JBoss repositories
In `~/.m2/settings.xml` add the following profiles:
```xml
<profile>
  <id>jboss-ga-repository</id>
  <repositories>
    <repository>
      <id>JBoss All GA</id>
      <url>http://maven.repository.redhat.com/techpreview/all</url>
      <releases>
        <enabled>true</enabled>
      </releases>
      <snapshots>
        <enabled>false</enabled>
      </snapshots>
    </repository>
  </repositories>
  <pluginRepositories>
    <pluginRepository>
      <id>JBoss All Plugins GA</id>
      <url>http://maven.repository.redhat.com/techpreview/all</url>
      <releases>
        <enabled>true</enabled>
      </releases>
      <snapshots>
        <enabled>false</enabled>
      </snapshots>
    </pluginRepository>
  </pluginRepositories>
</profile>
<profile>
  <id>jboss</id>
  <repositories>
    <repository>
      <id>archetype</id>
      <url>http://repository.jboss.org/nexus/content/groups/public</url>
      <releases>
        <enabled>true</enabled>
        <checksumPolicy>fail</checksumPolicy>
      </releases>
      <snapshots>
        <enabled>true</enabled>
        <checksumPolicy>warn</checksumPolicy>
      </snapshots>
    </repository>
  </repositories>
</profile>
```

## Configure Infinispan server
1. This example uses JDBC to store the cache. To permit this, it's necessary to alter Infinispan configuration file `INFINISPAN_HOME/standalone/configuration/standalone.xml` to contain the followin definitions:

```xml
<subsystem xmlns="urn:jboss:domain:datasources:4.0">
    <!-- Define this Datasource with jndi name  java:jboss/datasources/ExampleDS -->
    <datasources>
        <datasource jndi-name="java:jboss/datasources/ExampleDS" pool-name="ExampleDS" enabled="true" use-java-context="true">
            <!-- The connection URL uses H2 Database Engine with in-memory database called test -->
            <connection-url>jdbc:h2:mem:test;DB_CLOSE_DELAY=-1</connection-url>
            <!-- JDBC driver name -->
            <driver>h2</driver>
            <!-- Credentials -->
            <security>
                <user-name>sa</user-name>
                <password>sa</password>
            </security>
        </datasource>
        <!-- Define the JDBC driver called 'h2' -->
        <drivers>
            <driver name="h2" module="com.h2database.h2">
                <xa-datasource-class>org.h2.jdbcx.JdbcDataSource</xa-datasource-class>
            </driver>
        </drivers>
    </datasources>
</subsystem>
```
2. Add a local-cache called "components" in Infinispan subsystem definition:
```xml
<subsystem xmlns="urn:infinispan:server:core:9.4" default-cache-container="local">
    <cache-container name="local" default-cache="default" statistics="true">
        <global-state />
        <local-cache name="default" />
        <local-cache name="namedCache" />

        <!-- ADD a local cache called 'components' -->
        <local-cache name="components" start="EAGER" batching="false">
            <!-- Define the JdbcBinaryCacheStores to point to the ExampleDS previously defined -->
            <string-keyed-jdbc-store datasource="java:jboss/datasources/ExampleDS" passivation="false" preload="false" purge="false">
                <!-- specifies information about database table/column names and data types -->
                <string-keyed-table prefix="infinispan">
                    <id-column name="id" type="VARCHAR" />
                    <data-column name="datum" type="BINARY" />
                    <timestamp-column name="version" type="BIGINT" />
                </string-keyed-table>
            </string-keyed-jdbc-store>
        </local-cache>
        <!-- End of local cache called 'components' definition -->
    </cache-container>
</subsystem>
```
3. Disable REST endpoint security: by default the standalone.xml configuration protects the REST endpoint with BASIC authentication. Since this example cannot perform authentication, the REST connector should be configured without it: under `<subsystem xmlns="urn:infinispan:server:endpoint:9.4">`:
```xml
<rest-connector socket-binding="rest" cache-container="local"/>
```

## Run the example

### Start Infinispan Server:
```shell
INFINISPAN_HOME/bin/standalone.sh
```

### Build and Run the example
1. Make sure you have started the infinispan server
2. Navigate to the root directory of this example
3. Run the example application in its directory:
```shell
mvn exec:java
```