package com.redhat.demo.optaplanner;

import java.io.IOException;
import java.util.Properties;

import com.redhat.demo.optaplanner.model.Cache;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.RemoteCounterManagerFactory;
import org.infinispan.client.hotrod.annotation.ClientCacheEntryCreated;
import org.infinispan.client.hotrod.annotation.ClientCacheEntryExpired;
import org.infinispan.client.hotrod.annotation.ClientCacheEntryModified;
import org.infinispan.client.hotrod.annotation.ClientCacheEntryRemoved;
import org.infinispan.client.hotrod.annotation.ClientListener;
import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;
import org.infinispan.client.hotrod.event.ClientEvent;
import org.infinispan.commons.configuration.XMLStringConfiguration;
import org.infinispan.counter.api.CounterEvent;
import org.infinispan.counter.api.CounterListener;
import org.infinispan.counter.api.CounterManager;
import org.infinispan.counter.api.StrongCounter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.PropertySource;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HelloController {

    @Autowired
    private SimpMessagingTemplate template;

    @Value("${infinispan.host}")
    private String infinispanHost;

    @Value("${infinispan.hotrod.port}")
    private int hotrodPort;

    @Bean
    private ClientConfiguration clientConfiguration() {
        return new ClientConfiguration(infinispanHost, hotrodPort);
    }

    @GetMapping("/")
    public String hello(Model model) {
        return "hello";
    }

    @MessageMapping("/cacheSubscribe")
    public void handleCacheSubscribtion(Cache cache) {
        ClientConfiguration configuration = clientConfiguration();
        System.out.println(configuration.getInfinispanHost() + ":" + configuration.getHotRodPort());
        System.out.println(cache.getCacheName());
        RemoteCacheManager remoteCacheManager = new RemoteCacheManager(configuration.getConfigurationBuilder().build());
//        RemoteCache<String, String> remoteCache = remoteCacheManager.administration().getOrCreateCache(cache.getCacheName(),
//                new XMLStringConfiguration(String.format(
//                        "<infinispan>" +
//                                "<cache-container>" +
//                                    "<distributed-cache name=\"%1$s\">" +
//                                        "<memory>" +
//                                            "<object size=\"10\"/>" +
//                                        "</memory>" +
//                                        "<persistence passivation=\"false\">" +
//                                            "<file-store " +
//                                                "shared=\"false\" " +
//                                                "fetch-state=\"true\" " +
//                                                "path=\"${jboss.server.data.dir}/datagrid-infinispan/%1$s\"" +
//                                            "/>" +
//                                        "</persistence>" +
//                                    "</distributed-cache>" +
//                                "</cache-container>" +
//                            "</infinispan>", cache.getCacheName())
//                ));
//        remoteCache.addClientListener(new RemoteListener());

        String counterName;
        Properties props = new Properties();
        try {
            props.load(HelloController.class.getClassLoader().getResourceAsStream("application.properties"));
            counterName = props.getProperty("health.counter.name");
        } catch (IOException e) {
            e.printStackTrace();
            throw new IllegalStateException("Could not load application.properties file");
        }

        System.out.println("HelloController.handleCacheSubscribtion: " + counterName);
        CounterManager counterManager = RemoteCounterManagerFactory.asCounterManager(remoteCacheManager);
        StrongCounter strongCounter = counterManager.getStrongCounter(counterName);
        strongCounter.addListener(new RemoteListener());
    }

    @ClientListener()
    private class RemoteListener implements CounterListener {

        @ClientCacheEntryCreated
        @ClientCacheEntryModified
        @ClientCacheEntryExpired
        @ClientCacheEntryRemoved
        public void handleRemoteEvent(ClientEvent event) {
            System.out.println("RemoteListener.handleRemoteEvent: " + event);
            template.convertAndSend("/topic/damageEvents", event);
        }

        @Override
        public void onUpdate(CounterEvent entry) {
            System.out.println("RemoteListener.onUpdate: " + entry);
        }
    }

}
