package org.redhatsummit.infinispan;

import java.io.IOException;
import java.util.Properties;

import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.annotation.ClientCacheEntryCreated;
import org.infinispan.client.hotrod.annotation.ClientCacheEntryModified;
import org.infinispan.client.hotrod.annotation.ClientListener;
import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;
import org.infinispan.client.hotrod.event.ClientCacheEntryCreatedEvent;
import org.infinispan.client.hotrod.event.ClientCacheEntryModifiedEvent;
import org.junit.jupiter.api.Test;
import org.redhatsummit.infinispan.domain.MachineComponent;

public class InfinispanRemoteHotRodClientTest {
    private static final String INFINISPAN_HOST = "infinispan.host";
    public static final String HOTROD_PORT = "infinispan.hotrod.port";
    private static final String PROPERTIES_FILE = "infinispan.properties";
    private static final String cacheName = "components";

    @Test
    public void addAndRetrieveComponentTest() throws InterruptedException {
        ConfigurationBuilder builder = new ConfigurationBuilder();
        builder.addServer()
                .host(infinispanProperty(INFINISPAN_HOST))
                .port(Integer.parseInt(infinispanProperty(HOTROD_PORT)));
        RemoteCacheManager cacheManager = new RemoteCacheManager(builder.build());
        RemoteCache<String, Object> cache = cacheManager.getCache(cacheName);
        cache.addClientListener(new RemoteListener2(cache));

        MachineComponent component = new MachineComponent(0L, 0.0);
        cache.put(component.getId().toString(), component);

        Thread.sleep(1000);

        MachineComponent loadedComponent = (MachineComponent) cache.get(component.getId().toString());
        assert component.equals(loadedComponent);
        cache.remove(component.getId());
    }

    private static String infinispanProperty(String name) {
        Properties props = new Properties();
        {
            try {
                props.load(InfinispanRemoteHotRodClientTest.class.getClassLoader().getResourceAsStream(PROPERTIES_FILE));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return props.getProperty(name);
        }
    }

    @ClientListener()
    private class RemoteListener2 {

        private RemoteCache<String, Object> remoteCache;

        public RemoteListener2(RemoteCache<String, Object> remoteCache) {
            this.remoteCache = remoteCache;
        }

        @ClientCacheEntryCreated
        public void handleRemoteEvent(ClientCacheEntryCreatedEvent<String> event) {
            System.out.println("RemoteListener.handleRemoteEvent: " + event);
            System.out.println(remoteCache.get(event.getKey()));
        }

        @ClientCacheEntryModified
        public void handleRemoteEvent(ClientCacheEntryModifiedEvent<String> event) {
            System.out.println("RemoteListener.handleRemoteEvent: " + event);
            System.out.println(remoteCache.get(event.getKey()));
        }
    }
}
