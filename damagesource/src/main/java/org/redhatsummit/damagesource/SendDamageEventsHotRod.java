package org.redhatsummit.damagesource;

import java.io.IOException;
import java.util.Map;
import java.util.Properties;

import io.netty.util.internal.ThreadLocalRandom;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;
import org.infinispan.commons.util.CloseableIteratorSet;

public class SendDamageEventsHotRod {
    private static final String INFINISPAN_HOST = "infinispan.host";
    private static final String HOTROD_PORT = "infinispan.hotrod.port";
    private static final String PROPERTIES_FILE = "infinispan.properties";
    private static final String cacheName = "DamageEventHotRod";

    private static int EVENTS_PER_SECOND = 1;

    public static void main(String[] args) throws Exception {
        ConfigurationBuilder builder = new ConfigurationBuilder();
        builder.addServer()
                .host(infinispanProperty(INFINISPAN_HOST))
                .port(Integer.parseInt(infinispanProperty(HOTROD_PORT)));
        RemoteCacheManager remoteCacheManager = new RemoteCacheManager(builder.build());
        RemoteCache<String, Object> remoteCache = remoteCacheManager.getCache(cacheName);
        remoteCache.addClientListener(new RemoteListener());

        queryCacheStore(remoteCache);

        Long id = 0L;

        while (true) {
            for (int i = 0; i < EVENTS_PER_SECOND; i++) {
                sendDamageEvent(remoteCache, id++);
            }
            Thread.sleep(1000);
        }
    }

    private static void sendDamageEvent(RemoteCache<String, Object> remoteCache, Long id) {
        DamageEvent damageEvent = new DamageEvent(id, ThreadLocalRandom.current().nextDouble());
        remoteCache.put(damageEvent.getId().toString(), damageEvent);
    }

    private static void queryCacheStore(RemoteCache<String, Object> remoteCache) {
        CloseableIteratorSet<Map.Entry<String, Object>> iterator = remoteCache.entrySet();
        iterator.forEach(System.out::println);
    }

    private static String infinispanProperty(String name) {
        Properties props = new Properties();
        try {
            props.load(SendDamageEventsHotRod.class.getClassLoader().getResourceAsStream(PROPERTIES_FILE));
        } catch (IOException e) {
            e.printStackTrace();
        }

        return props.getProperty(name);
    }

}
