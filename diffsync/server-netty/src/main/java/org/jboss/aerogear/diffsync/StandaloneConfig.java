package org.jboss.aerogear.diffsync;

public class StandaloneConfig {

    private final String host;
    private final int port;

    private StandaloneConfig(final Builder builder) {
        host = builder.host;
        port = builder.port;
    }

    public String host() {
        return host;
    }

    public int port() {
        return port;
    }

    @Override
    public String toString() {
        return "StandaloneConfig[host=" + host + ", port=" + port + ']';
    }

    public static Builder host(final String host) {
        return new Builder(host);
    }

    public static class Builder {
        private String host;
        private int port;

        public Builder(final String host) {
            this.host = host;
        }

        public Builder port(final int port) {
            this.port = port;
            return this;
        }

        public StandaloneConfig build() {
            return new StandaloneConfig(this);
        }

    }
}
