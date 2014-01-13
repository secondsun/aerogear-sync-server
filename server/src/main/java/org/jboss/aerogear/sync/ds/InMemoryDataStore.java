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

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class InMemoryDataStore implements DataStore<String> {

    private ConcurrentMap<String, Document<String>> documents = new ConcurrentHashMap<String, Document<String>>();
    private ConcurrentMap<String, ShadowDocument<String>> shadows = new ConcurrentHashMap<String, ShadowDocument<String>>();
    private ConcurrentMap<String, Edits> edits = new ConcurrentHashMap<String, Edits>();

    @Override
    public void saveShadowDocument(final ShadowDocument shadowDocument) {
        shadows.put(shadowDocument.document().id(), shadowDocument);
    }

    @Override
    public ShadowDocument getShadowDocument(final String documentId) {
        return shadows.get(documentId);
    }

    @Override
    public void saveBackupShadowDocument(final BackupShadowDocument backupShadow) {

    }

    @Override
    public BackupShadowDocument getBackupShadowDocument(final String documentId) {
        return null;
    }

    @Override
    public void saveDocument(final Document document) {

    }

    @Override
    public Document getDocument(final String documentId) {
        return null;
    }

    @Override
    public void saveEdits(final Edits edits, final String documentId) {
        this.edits.put(documentId, edits);
    }

    @Override
    public Edits getEdit(final String documentId) {
        return null;
    }
}
