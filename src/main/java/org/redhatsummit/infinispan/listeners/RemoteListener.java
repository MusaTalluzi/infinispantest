package org.redhatsummit.infinispan.listeners;

import org.infinispan.client.hotrod.annotation.ClientCacheEntryCreated;
import org.infinispan.client.hotrod.annotation.ClientCacheEntryModified;
import org.infinispan.client.hotrod.annotation.ClientListener;
import org.infinispan.client.hotrod.event.ClientCacheEntryCreatedEvent;
import org.infinispan.client.hotrod.event.ClientCacheEntryModifiedEvent;

@ClientListener
public class RemoteListener {

    @ClientCacheEntryCreated
    public void entryCreated(ClientCacheEntryCreatedEvent<String> event) {
        System.out.println("Created " + event.getKey());
    }

    @ClientCacheEntryModified
    public void entryModified(ClientCacheEntryModifiedEvent<String> event) {
        System.out.println("About to modify " + event.getKey());
    }
}
