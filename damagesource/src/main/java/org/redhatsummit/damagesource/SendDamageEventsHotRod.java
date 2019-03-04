package org.redhatsummit.damagesource;

import java.time.Instant;
import java.util.Map;
import java.util.Properties;
import java.util.Random;

import io.netty.util.internal.ThreadLocalRandom;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.RemoteCounterManagerFactory;
import org.infinispan.client.hotrod.configuration.Configuration;
import org.infinispan.commons.configuration.XMLStringConfiguration;
import org.infinispan.commons.util.CloseableIteratorSet;
import org.infinispan.counter.api.CounterConfiguration;
import org.infinispan.counter.api.CounterManager;
import org.infinispan.counter.api.CounterType;
import org.infinispan.counter.api.StrongCounter;

public class SendDamageEventsHotRod {

    private static final String cacheName = "ComponentHealth";
    private static final Random random = new Random();

    public static void main(String[] args) throws Exception {
        Configuration configuration = ClientConfiguration.get().build();
        RemoteCacheManager remoteCacheManager = new RemoteCacheManager(configuration);
//        RemoteCache<String, String> remoteCache = remoteCacheManager.administration().getOrCreateCache(cacheName,
//                new XMLStringConfiguration(String.format(
//                        "<infinispan>" +
//                            "<cache-container>" +
//                                "<distributed-cache name=\"%1$s\">" +
//                                    "<memory>" +
//                                        "<object size=\"10\"/>" +
//                                    "</memory>" +
//                                    "<persistence passivation=\"false\">" +
//                                        "<file-store " +
//                                            "shared=\"false\" " +
//                                            "fetch-state=\"true\" " +
//                                            "path=\"${jboss.server.data.dir}/datagrid-infinispan/%1$s\"" +
//                                        "/>" +
//                                    "</persistence>" +
//                                "</distributed-cache>" +
//                            "</cache-container>" +
//                         "</infinispan>", cacheName)
//                ));
//        remoteCache.addClientListener(new RemoteListener());

        Properties props = new Properties();
        props.load(SendDamageEventsHotRod.class.getClassLoader().getResourceAsStream("infinispan.properties"));
        int eventsPerSecond = Integer.parseInt(props.getProperty("damage.events.per.second"));
        String healthCounterName = props.getProperty("health.counter.name");
        int initialValue = Integer.parseInt(props.getProperty("health.counter.initial.value"));
        int lowerBound = Integer.parseInt(props.getProperty("health.counter.lower.bound"));

        CounterManager counterManager = RemoteCounterManagerFactory.asCounterManager(remoteCacheManager);
        CounterConfiguration cc = CounterConfiguration.builder(CounterType.BOUNDED_STRONG)
                .initialValue(initialValue)
                .lowerBound(lowerBound)
                .build();

        counterManager.defineCounter(healthCounterName, cc);

        StrongCounter strongCounter = counterManager.getStrongCounter(healthCounterName);

        strongCounter.getValue().thenAccept(System.out::println);

        random.setSeed(13);
        while (true) {
            for (int i = 0; i < eventsPerSecond; i++) {
                int decremntBy = random.nextInt(initialValue) % 10;
                strongCounter.addAndGet((long) decremntBy * -1).thenAccept(System.out::println);
            }
            Thread.sleep(1000);
        }
//        Long id = 0L;
//        while (true) {
//            for (int i = 0; i < eventsPerSecond; i++) {
//                sendDamageEvent(remoteCache, id++);
//            }
//            Thread.sleep(1000);
//        }
    }

//    private static void sendDamageEvent(RemoteCache<String, Object> remoteCache, Long id) {
//        DamageEvent damageEvent = new DamageEvent(id, ThreadLocalRandom.current().nextDouble());
//        remoteCache.put(damageEvent.getId().toString(), damageEvent);
//    }
//
//    private static void queryCacheStore(RemoteCache<String, Object> remoteCache) {
//        CloseableIteratorSet<Map.Entry<String, Object>> iterator = remoteCache.entrySet();
//        iterator.forEach(System.out::println);
//    }
}
