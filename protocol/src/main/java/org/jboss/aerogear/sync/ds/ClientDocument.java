package org.jboss.aerogear.sync.ds;

/**
 * A ClientDocument is a representation of a client document which can exist either
 * as a specific document or shadow document.
 *
 * @param <T> the type of this documents content.
 */
public interface ClientDocument<T> extends Document<T> {

    /**
     * Identifies a client or session to whom this Document belongs.
     *
     * @return {@code String} the client identifier.
     */
    String clientId();
}
