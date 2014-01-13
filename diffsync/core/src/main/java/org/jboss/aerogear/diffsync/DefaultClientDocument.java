package org.jboss.aerogear.diffsync;

public class DefaultClientDocument<T> extends DefaultDocument<T> implements ClientDocument<T> {

    private final String clientId;

    public DefaultClientDocument(String id, String clientId, T content) {
        super(id, content);
        this.clientId = clientId;
    }

    @Override
    public String clientId() {
        return clientId;
    }
    
    @Override
    public String toString() {
        return "DefaultClientDocument[id=" + id() + ", clientId=" + clientId + ", content=" + content() + ']';
    }
}
