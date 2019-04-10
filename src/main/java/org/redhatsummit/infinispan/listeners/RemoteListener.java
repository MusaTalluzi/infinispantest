package org.redhatsummit.infinispan.listeners;

import org.infinispan.client.hotrod.annotation.ClientCacheEntryCreated;
import org.infinispan.client.hotrod.annotation.ClientCacheEntryExpired;
import org.infinispan.client.hotrod.annotation.ClientCacheEntryModified;
import org.infinispan.client.hotrod.annotation.ClientCacheEntryRemoved;
import org.infinispan.client.hotrod.annotation.ClientListener;
import org.infinispan.client.hotrod.event.ClientCacheEntryCreatedEvent;
import org.infinispan.client.hotrod.event.ClientCacheEntryModifiedEvent;
import org.infinispan.client.hotrod.event.ClientEvent;

@ClientListener()
public class RemoteListener {

    @ClientCacheEntryCreated
    public void handleRemoteEvent(ClientCacheEntryCreatedEvent event) {
        System.out.println("RemoteListener.handleRemoteEvent: " + event);
        System.out.println("RemoteListener.handleRemoteEvent: " + event.getType());
    }

    @ClientCacheEntryModified
    public void handleRemoteEvent(ClientCacheEntryModifiedEvent event) {
        System.out.println("RemoteListener.handleRemoteEvent: " + event);
        System.out.println("RemoteListener.handleRemoteEvent: " + event.getType());
        System.out.println("RemoteListener.handleRemoteEvent: " + event.getKey().toString());
        System.out.println("RemoteListener.handleRemoteEvent: " + event.getVersion());
    }
}
