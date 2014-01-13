package org.jboss.aerogear.diffsync;

import org.junit.Test;

public class DiffSyncClientIntegrationTest {
    
    @Test
    public void connect() throws InterruptedException {
        final String documentId = "1234";
        final String clientId = "client2";
        final String originalVersion = "{\"id\": 9999}";
        
        final DiffSyncClient<String> client = DiffSyncClient.<String>forHost("localhost").port(7777).path("/sync").build();
        client.connect();
        client.addDocument(clientDoc(documentId, clientId, originalVersion));
        Thread.sleep(1000);
        client.disconnect();
    }
    
    private static ClientDocument<String> clientDoc(final String docId, final String clientId, final String content) {
        return new DefaultClientDocument<String>(docId, clientId, content);
    }
  

}