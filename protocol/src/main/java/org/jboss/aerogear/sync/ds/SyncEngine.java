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
package org.jboss.aerogear.sync.ds;

public interface SyncEngine<T> {

    /**
     * Called when the shadow should be patched. Is called when an update is recieved.
     *
     * @param edit The edit.
     * @return
     */
    Document<T> patchShadow(Edit edit);

    /**
     *
     * @param edit
     * @return
     */
    Document<T> patchDocument(Edit edit);

    /**
     * The first step in a sync is to produce a an edit for the changes.
     *
     * @param document the document containing
     * @param shadowDocument
     * @return
     */
    Edit diff(Document<T> document, ShadowDocument<T> shadowDocument);




}
