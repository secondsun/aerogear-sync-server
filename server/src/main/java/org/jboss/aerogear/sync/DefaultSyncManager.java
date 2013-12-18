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

import org.ektorp.http.HttpClient;
import org.ektorp.http.StdHttpClient;
import org.ektorp.impl.StdCouchDbConnector;
import org.ektorp.impl.StdCouchDbInstance;

import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.UUID;

public class DefaultSyncManager implements SyncManager {

    private final HttpClient httpClient;
    private final StdCouchDbInstance stdCouchDbInstance;
    private final StdCouchDbConnector db;

    public DefaultSyncManager(final String url, final String dbName)  {
        try {
            httpClient = new StdHttpClient.Builder().url(url).build();
        } catch (final MalformedURLException e) {
            throw new IllegalStateException(e);
        }
        stdCouchDbInstance = new StdCouchDbInstance(httpClient);
        db = new StdCouchDbConnector(dbName, stdCouchDbInstance);
        db.createDatabaseIfNotExists();
    }

    @Override
    public Document read(final String id, final String revision) {
        return null;
    }

    @Override
    public Document create(final String json) {
        final HashMap<String, String> map = new HashMap<String, String>();
        map.put("_id", UUID.randomUUID().toString());
        map.put("content", json);
        db.create(map);
        return new Document() {
            @Override
            public int id() {
                return Integer.valueOf(map.get("_id"));
            }
            @Override
            public int revision() {
                return Integer.valueOf(map.get("_rev"));
            }
            @Override
            public String content() {
                return map.get("content");
            }
        };
    }

    @Override
    public Document update(final Document doc) {
        return null;
    }

    @Override
    public Document delete(final String revision) {
        return null;
    }
}
