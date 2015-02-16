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
package org.jboss.aerogear.sync.client;

import org.jboss.aerogear.sync.ClientDocument;

/**
 * A listener/observer of patches.
 *
 * @param <T> The content type of {@link ClientDocument} that this PatchListener listens to.
 */
public interface PatchListener<T> {

    /**
     * Is called when the {@link ClientDocument} has patched a document.
     *
     * @param patchedDocument the {@link ClientDocument} that has been patched.
     */
    void patched(ClientDocument<T> patchedDocument);
}
