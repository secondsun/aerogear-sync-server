package org.jboss.aerogear.sync;

import java.util.Queue;

public class DefaultPatchMessage implements PatchMessage<DefaultEdit> {

    private final String documentId;
    private final String clientId;
    private final Queue<DefaultEdit> edits;

    public DefaultPatchMessage(final String documentId, final String clientId, final Queue<DefaultEdit> edits) {
        this.documentId = documentId;
        this.clientId = clientId;
        this.edits = edits;
    }

    @Override
    public String documentId() {
        return documentId;
    }

    @Override
    public String clientId() {
        return clientId;
    }

    @Override
    public Queue<DefaultEdit> edits() {
        return edits;
    }

    @Override
    public String toString() {
        return "DefaultEdits[documentId=" + documentId + ", clientId=" + clientId + ", edits=" + edits + ']';
    }
}
