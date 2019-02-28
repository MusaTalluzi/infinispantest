# An example using Remote Access to cache data using an Infinispan Server via HotRod

## System requirements
- Java 8.0^ (Java SDK 1.8)
- Maven 3.0^
- [Infinispan Server 9.4.6.Final](http://downloads.jboss.org/infinispan/9.4.6.Final/infinispan-server-9.4.6.Final.zip).

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
```

## Configure Infinispan Server
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

## Run the example

### Start Infinispan Server:
```shell
INFINISPAN_HOME/bin/standalone.sh
```
Note: if you already have the environment variable $JBOSS_HOME set to another instance of Java AS (e.g. WildFly), make sure to unset this variable in the terminal instance running the infinispan server by typing `unset JBOSS_HOME`.

### Build and Run the example
1. Make sure you have started the infinispan server
2. Navigate to the root directory of this example
3. Build and deploy the archive
```shell
mvn clean package
```
4. Run the example application in its directory
```shell
mvn exec:java
```

# Use Hibernate OGM
Since Hibernate OGM uses a distributed-cache by default, run the server with `clustered.xml` configuration.
```shell
INFINISPAN_HOME/bin/standalone.sh -c clustered.xml
```
# Test listening to cache events:
We will test sending damageEvents from one application via HotRod protocol and listening to them from another application. To test it out:
1. Configure the cache that will store damageEvents `clustered.xml`:
```xml
<local-cache name="DamageEventHotRod" start="EAGER" batching="false">
    <persistence passivation="false">
        <string-keyed-jdbc-store name="STRING_KEYED_JDBC_STORE" datasource="java:jboss/datasources/ExampleDS" preload="false" purge="false">
            <string-keyed-table prefix="infinispan">
                <id-column name="id" type="VARCHAR" />
                <data-column name="datum" type="BINARY" />
                <timestamp-column name="version" type="BIGINT" />
            </string-keyed-table>
        </string-keyed-jdbc-store>
    </persistence>
</local-cache>
```
2. Run the Infinispan server:
```shell
INFINISPAN_HOME/bin/standalone.sh -c clustered.xml
```
3. Start `optaplanner-app`: Web app built spring boot. You can start it either from an IDE or from the command line
- `mvn clean package`
- `java -jar optaplanner-demo.jar`
4. Open browser on http://localhost:8081. Add the cache name `DamageEventHotRod` and click `listen`. This is the cahce name you configured in `clustered.xml`.
5. Run `damagesourceapp`:
- `mvn clean package`
- `mvn exec:java`
> If you run it from an IDE, run `org.redhatsummit.damagesource.SendDamageEventsHotRod`.

Once you start sending damage events, you will see them in the browser. By default, one damage event is sent per second, to modify this set `EVENTS_PER_SECOND` in `org.redhatsummit.damagesource.SendDamageEventsHotRod`.