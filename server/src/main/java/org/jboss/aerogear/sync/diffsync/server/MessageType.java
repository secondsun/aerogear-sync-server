package org.jboss.aerogear.sync.diffsync.server;

import java.util.HashMap;
import java.util.Map;

enum MessageType {
    ADD,
    EDITS,
    DETACH,
    UNKNOWN;

    private static final Map<String, MessageType> MAP;
    static {
        final Map<String, MessageType> map = new HashMap<String, MessageType>();
        for (MessageType type : values()) {
            final String name = type.name();
            if (name != null) {
                map.put(name, type);
            }
        }
        MAP = map;
    }

    public static MessageType from(final String name) {
        final MessageType type = MAP.get(name.toUpperCase());
        return type == null ? UNKNOWN : type;
    }
}
