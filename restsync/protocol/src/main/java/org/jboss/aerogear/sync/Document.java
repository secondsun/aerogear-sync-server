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
package org.jboss.aerogear.sync;

/**
 * Defines the structure of a document for the synchronization protocol.
 */
public interface Document {

    /**
     * The identifier for a document.
     *
     * @return {@code String} the document identifier.
     */
    String id();

    /**
     * Returns the documents revision.
     *
     * @return {@code String} the document revision.
     */
    String revision();

    /**
     * The documents content
     *
     * @return {@code String} the contents of the document.
     */
    String content();

}
