package org.jboss.aerogear.diffsync;

/**
 * A client document is used on both the server and client side and
 * associates a client identifier with a {@link Document}.
 *
 * @param <T> the type of this documents content.
 */
public interface ClientDocument<T> extends Document<T> {

    /**
     * Identifies a client or session to which this Document belongs.
     *
     * @return {@code String} the client identifier.
     */
    String clientId();
}
