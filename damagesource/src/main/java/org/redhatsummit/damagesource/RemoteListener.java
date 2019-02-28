package org.redhatsummit.damagesource;

import org.infinispan.client.hotrod.annotation.ClientCacheEntryCreated;
import org.infinispan.client.hotrod.annotation.ClientCacheEntryExpired;
import org.infinispan.client.hotrod.annotation.ClientCacheEntryModified;
import org.infinispan.client.hotrod.annotation.ClientCacheEntryRemoved;
import org.infinispan.client.hotrod.annotation.ClientListener;
import org.infinispan.client.hotrod.event.ClientEvent;

@ClientListener()
public class RemoteListener {

    @ClientCacheEntryCreated
    @ClientCacheEntryModified
    @ClientCacheEntryExpired
    @ClientCacheEntryRemoved
    public void handleRemoteEvent(ClientEvent event) {
        System.out.println("RemoteListener.handleRemoteEvent: " + event);
    }
}
