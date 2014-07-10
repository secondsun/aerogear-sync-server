/**
 * JBoss, Home of Professional Open Source
 * Copyright Red Hat, Inc., and individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 	http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.aerogear.sync.diffsync;

import java.util.Stack;

/**
 * A sync message represents a synchronization message between the server and the client.
 */
public interface SyncMessage {

    /**
     * The version of the document that the {@link Edit}s in this message are based on.
     * <p>
     *
     * Client side {@link Edit}s are produced by taking the shadow on the client side, and the client's latest version
     * of the document and producing a diff.
     * For clients sending a SyncMessage the version returned by this method will be the servers version. This is the
     * latest version of the server document that the client has "seen".
     *
     * Server side {@link Edit}s are produced by taking the client's server side shadow, and the server latest version
     * and producing a diff.
     * When the server is sending a SyncMessage the version returned by this method will be the clients version. This is
     * the latest version of the clients document that the server has "seen".
     *
     * @return {@code long} the latest version of the other sides document.
     */
    long version();

    /**
     * A stack of edits that are to be applied to the target document, which can
     * be either a client or server side document.
     * <p>
     * The stack may be emtpy which is possible if this sync message is coming from a client that has not made
     * any changes. The sync message in this case will only contain the servers version number which then acts as
     * an acknowledgment that the client has received that version from the server.
     *
     * @return {@code Stack} containing zero or more edits.
     */
    Stack<Edit> edits();
}
