package com.redhat.demo.optaplanner;

import com.redhat.demo.optaplanner.model.Cache;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.annotation.ClientCacheEntryCreated;
import org.infinispan.client.hotrod.annotation.ClientCacheEntryExpired;
import org.infinispan.client.hotrod.annotation.ClientCacheEntryModified;
import org.infinispan.client.hotrod.annotation.ClientCacheEntryRemoved;
import org.infinispan.client.hotrod.annotation.ClientListener;
import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;
import org.infinispan.client.hotrod.event.ClientEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HelloController {

    @Autowired
    private SimpMessagingTemplate template;

    @GetMapping("/")
    public String hello(Model model) {
        return "hello";
    }

    @MessageMapping("/cacheSubscribe")
    public void handleCacheSubscribtion(Cache cache) {
        System.out.println(cache.getCacheName());
        ConfigurationBuilder builder = new ConfigurationBuilder();
        builder.addServer()
                .host("localhost")
                .port(11222);
        RemoteCacheManager remoteCacheManager = new RemoteCacheManager(builder.build());
        RemoteCache<String, Object> remoteCache = remoteCacheManager.getCache(cache.getCacheName());
        remoteCache.addClientListener(new RemoteListener());
    }

    @ClientListener()
    private class RemoteListener {

        @ClientCacheEntryCreated
        @ClientCacheEntryModified
        @ClientCacheEntryExpired
        @ClientCacheEntryRemoved
        public void handleRemoteEvent(ClientEvent event) {
            System.out.println("RemoteListener.handleRemoteEvent: " + event);
            template.convertAndSend("/topic/damageEvents", event);
        }
    }

}
