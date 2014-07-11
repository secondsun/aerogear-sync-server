package org.jboss.aerogear.sync.diffsync;

import java.util.Queue;

public class Edits {

    private final static String MSG_TYPE = "patch";
    private final Queue<Edit> edits;

    public Edits(final Queue<Edit> edits) {
        this.edits = edits;
    }

    public Queue<Edit> getEdits() {
        return edits;
    }

    public String msgType() {
        return MSG_TYPE;
    }
}
