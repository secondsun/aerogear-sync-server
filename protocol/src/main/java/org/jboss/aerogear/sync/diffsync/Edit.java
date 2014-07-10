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

import java.util.LinkedList;

/**
 * Represents a single edit.
 */
public interface Edit {

    /**
     * Identifies the client that this edit instance belongs to.
     *
     * @return {@code String} the client identifier.
     */
    String clientId();

    /**
     * Identifies the document that this edit is related to
     *
     * @return {@code String} the document documentId.
     */
    String documentId();

    /**
     * The client version that edit is related to.
     *
     * @return {@code long} the client version that this edit is based on.
     */
    long clientVersion();

    /**
     * The server version that edit is related to.
     *
     * @return {@code long} the server version that this edit is based on.
     */
    long serverVersion();

    /**
     * A checksum of the opposing sides, client or server, shadow document.
     * The shadow document must patch strictly and this checksum is used to verify that the other sides
     * shadow document is in fact the same. This can then be used by when before patching to make sure that
     * the shadow documents on both sides are in fact identical.
     *
     * @return {@code String} the opposing sides checksum of the shadow document
     */
    String checksum();

    /**
     * The diff for this edit.
     *
     * @return {@code String} the diff that represents the changes for this edit.
     */
    LinkedList<Diff> diffs();

}
