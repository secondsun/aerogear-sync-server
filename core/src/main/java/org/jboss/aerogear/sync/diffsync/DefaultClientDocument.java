package org.jboss.aerogear.sync.diffsync;

public class DefaultClientDocument<T> extends DefaultDocument<T> implements ClientDocument<T> {

    private final String clientId;

    public DefaultClientDocument(String id, T content, String clientId) {
        super(id, content);
        this.clientId = clientId;
    }

    @Override
    public String clientId() {
        return clientId;
    }
}
