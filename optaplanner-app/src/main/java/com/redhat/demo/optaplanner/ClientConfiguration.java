package com.redhat.demo.optaplanner;

import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;

public class ClientConfiguration {
    private String infinispanHost;
    private int hotRodPort;

    public ClientConfiguration() {
    }

    public ClientConfiguration(String infinispanHost, int hotrodPort) {
        this.infinispanHost = infinispanHost;
        this.hotRodPort = hotrodPort;
    }

    public String getInfinispanHost() {
        return infinispanHost;
    }

    public void setInfinispanHost(String infinispanHost) {
        this.infinispanHost = infinispanHost;
    }

    public int getHotRodPort() {
        return hotRodPort;
    }

    public void setHotRodPort(int hotRodPort) {
        this.hotRodPort = hotRodPort;
    }

    public ConfigurationBuilder getConfigurationBuilder() {
        ConfigurationBuilder builder = new ConfigurationBuilder();
        builder.addServer()
                .host(infinispanHost)
                .port(hotRodPort);
//                .security().authentication()
//                .enable()
//                .username(USERNAME)
//                .password(PASSWORD)
//                .realm("ApplicationRealm")
//                .serverName(HOTROD_SERVICE_NAME)
//                .saslMechanism("DIGEST-MD5")
//                .saslQop(SaslQop.AUTH);
        return builder;
    }


}
