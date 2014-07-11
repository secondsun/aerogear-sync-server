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
package org.jboss.aerogear.sync.diffsync.server;

import org.jboss.aerogear.sync.diffsync.BackupShadowDocument;
import org.jboss.aerogear.sync.diffsync.ClientDocument;
import org.jboss.aerogear.sync.diffsync.Document;
import org.jboss.aerogear.sync.diffsync.Edit;
import org.jboss.aerogear.sync.diffsync.ShadowDocument;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;

public class ServerInMemoryDataStore implements ServerDataStore<String> {

    private static final Queue<Edit> EMPTY_QUEUE = new LinkedList<Edit>();
    private final ConcurrentMap<String, Document<String>> documents = new ConcurrentHashMap<String, Document<String>>();
    private final ConcurrentMap<Id, ShadowDocument<String>> shadows = new ConcurrentHashMap<Id, ShadowDocument<String>>();
    private final ConcurrentMap<Id, BackupShadowDocument<String>> backups = new ConcurrentHashMap<Id, BackupShadowDocument<String>>();
    private final ConcurrentHashMap<Id, Queue<Edit>> pendingEdits = new ConcurrentHashMap<Id, Queue<Edit>>();

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
    public BackupShadowDocument<String> getBackupShadowDocument(final String documentId, final String clientId) {
        return backups.get(id(clientId, documentId));
    }

    @Override
    public void saveDocument(final Document<String> document) {
        documents.put(document.id(), document);
    }

    @Override
    public Document<String> getDocument(final String documentId) {
        return documents.get(documentId);
    }

    @Override
    public void saveEdits(final Edit edit) {
        final Id id = id(edit.clientId(), edit.documentId());
        final Queue<Edit> newEdits = new ConcurrentLinkedQueue<Edit>();
        newEdits.add(edit);
        while (true) {
            final Queue<Edit> currentEdits = pendingEdits.get(id);
            if (currentEdits == null) {
                final Queue<Edit> previous = pendingEdits.putIfAbsent(id, newEdits);
                if (previous != null) {
                    newEdits.addAll(previous);
                    if (pendingEdits.replace(id, previous, newEdits)) {
                        break;
                    }
                } else {
                    break;
                }
            } else {
                newEdits.addAll(currentEdits);
                if (pendingEdits.replace(id, currentEdits, newEdits)) {
                    break;
                }
            }
        }
    }

    @Override
    public void removeEdit(final Edit edit) {
        final Id id = id(edit.clientId(), edit.documentId());
        while (true) {
            final Queue<Edit> currentEdits = pendingEdits.get(id);
            if (currentEdits == null) {
                break;
            }
            final Queue<Edit> newEdits = new ConcurrentLinkedQueue<Edit>();
            newEdits.addAll(currentEdits);
            for (Iterator<Edit> iter = newEdits.iterator(); iter.hasNext();) {
                final Edit oldEdit = iter.next();
                if (oldEdit.clientVersion() <= edit.clientVersion()) {
                    iter.remove();
                }
            }
            if (pendingEdits.replace(id, currentEdits, newEdits)) {
                break;
            }
        }
    }

    @Override
    public Queue<Edit> getEdits(final String clientId, final String documentId) {
        final Queue<Edit> edits = pendingEdits.get(id(clientId, documentId));
        if (edits == null) {
            return EMPTY_QUEUE;
        }
        return edits;
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

        Id(final String clientId, final String documentId) {
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
