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

public interface PatchObservable<T> {

    /**
     * Adds the specified {@link PatchListener} to the set of listeners.
     *
     * @param listener the {@link PatchListener} to be added.
     */
    void addPatchListener(PatchListener<T> listener);

    /**
     * Remove the specified {@link PatchListener} from the set of listeners.
     *
     * @param listener the {@link PatchListener} to be removed.
     */
    void removePatchListener(PatchListener<T> listener);

    /**
     * Remove all {@link PatchListener}s.
     */
    void removePatchListeners();

    /**
     * Notify all listeners that the {@link ClientDocument} has been patched.
     *
     * @param patchedDocument the {@link ClientDocument} that has been patched.
     */
    void notifyPatched(ClientDocument<T> patchedDocument);

    /**
     * Marks this {@code PatchObservable} as having been changed
     */
    void changed();

    /**
     * Returns the number of currently registered {@link PatchListener}s
     *
     * @return {@code int} the number of listeners currently registered.
     */
    int countPatchListeners();
}
