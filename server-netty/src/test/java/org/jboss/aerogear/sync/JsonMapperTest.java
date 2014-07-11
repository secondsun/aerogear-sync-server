package org.jboss.aerogear.sync;

import org.jboss.aerogear.sync.diffsync.DefaultClientDocument;
import org.jboss.aerogear.sync.diffsync.Edits;
import org.jboss.aerogear.sync.diffsync.client.ClientDataStore;
import org.jboss.aerogear.sync.diffsync.client.ClientInMemoryDataStore;
import org.jboss.aerogear.sync.diffsync.client.ClientSyncEngine;
import org.jboss.aerogear.sync.diffsync.client.DefaultClientSynchronizer;
import org.junit.Test;

public class JsonMapperTest {

    @Test
    public void parseEdits() {
        final Edits edits = generateClientSideEdits("1234", "version1", "client1", "version2");
        final String json = JsonMapper.toJson(edits);
        System.out.println(json);
        final Edits deserialized = JsonMapper.fromJson(json, Edits.class);
        System.out.println(deserialized);
    }

    @Test
    public void edit() {
        final Edits edits = generateClientSideEdits("1234", "version1", "client1", "version2");
        final String json = JsonMapper.toJson(edits.getEdits().peek());
        System.out.println(json);
    }

    private static Edits generateClientSideEdits(final String documentId,
                                                       final String originalContent,
                                                       final String clientId,
                                                       final String updatedContent) {
        final ClientDataStore<String> clientDataStore = new ClientInMemoryDataStore();
        final ClientSyncEngine<String> clientSyncEngine = new ClientSyncEngine<String>(new DefaultClientSynchronizer(),
                clientDataStore);
        clientSyncEngine.addDocument(new DefaultClientDocument<String>(documentId, originalContent, clientId));
        return new Edits(clientSyncEngine.diff(new DefaultClientDocument<String>(documentId, updatedContent, clientId)));
    }
}
