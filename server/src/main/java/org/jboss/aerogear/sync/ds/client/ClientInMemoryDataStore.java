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
package org.jboss.aerogear.sync.ds.client;

import org.jboss.aerogear.sync.ds.BackupShadowDocument;
import org.jboss.aerogear.sync.ds.ClientDocument;
import org.jboss.aerogear.sync.ds.Document;
import org.jboss.aerogear.sync.ds.Edits;
import org.jboss.aerogear.sync.ds.ShadowDocument;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class ClientInMemoryDataStore implements ClientDataStore<String> {

    private final ConcurrentMap<Id, ClientDocument<String>> documents = new ConcurrentHashMap<Id, ClientDocument<String>>();
    private final ConcurrentMap<Id, ShadowDocument<String>> shadows = new ConcurrentHashMap<Id, ShadowDocument<String>>();
    private final ConcurrentMap<Id, BackupShadowDocument<String>> backups = new ConcurrentHashMap<Id, BackupShadowDocument<String>>();
    private final ConcurrentMap<Id, Edits> edits = new ConcurrentHashMap<Id, Edits>();

    @Override
    public void saveShadowDocument(final ShadowDocument<String> shadowDocument) {
        shadows.put(id(shadowDocument.document()), shadowDocument);
    }

    @Override
    public ShadowDocument<String> getShadowDocument(final String documentId, final String clientId) {
        return shadows.get(id(clientId, documentId));
    }

    @Override
    public void saveBackupShadowDocument(final BackupShadowDocument<String> backupShadow) {
        backups.put(id(backupShadow.shadow().document()), backupShadow);
    }

    @Override
    public BackupShadowDocument<String> getBackupShadowDocument(final String clientId, final String documentId) {
        return backups.get(id(clientId, documentId));
    }

    @Override
    public void saveClientDocument(final ClientDocument<String> document) {
        documents.put(id(document), document);
    }

    @Override
    public ClientDocument<String> getClientDocument(final String clientId, final String documentId) {
        return documents.get(id(clientId, documentId));
    }

    @Override
    public void saveEdits(final Edits edits, final Document<String> document) {
        this.edits.put(id(edits.clientId(), document.id()), edits);
    }

    @Override
    public Edits getEdit(final String clientId, final String documentId) {
        return edits.get(id(clientId, documentId));
    }

    private static Id id(final ClientDocument<String> document) {
        return id(document.clientId(), document.id());
    }

    private static Id id(final String clientId, final String documentId) {
        return new Id(clientId, documentId);
    }

    private static class Id {

        private final String clientId;
        private final String documentId;

        private Id(final String clientId, final String documentId) {
            this.clientId = clientId;
            this.documentId = documentId;
        }

        public String clientId() {
            return clientId;
        }

        public String getDocumentId() {
            return documentId;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof Id)) {
                return false;
            }

            final Id id = (Id) o;

            if (clientId != null ? !clientId.equals(id.clientId) : id.clientId != null) {
                return false;
            }
            if (documentId != null ? !documentId.equals(id.documentId) : id.documentId != null) {
                return false;
            }
            return true;
        }

        @Override
        public int hashCode() {
            int result = clientId != null ? clientId.hashCode() : 0;
            result = 31 * result + (documentId != null ? documentId.hashCode() : 0);
            return result;
        }
    }
}
