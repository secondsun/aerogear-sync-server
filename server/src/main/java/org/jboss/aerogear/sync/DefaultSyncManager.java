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

import org.ektorp.UpdateConflictException;
import org.ektorp.http.HttpClient;
import org.ektorp.http.StdHttpClient;
import org.ektorp.impl.StdCouchDbConnector;
import org.ektorp.impl.StdCouchDbInstance;

import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map;

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
    @SuppressWarnings("unchecked")
    public Document read(final String id) throws DocumentNotFoundException {
        try {
            final Map<String, String> map = db.get(Map.class, id);
            return asDocument(map);
        } catch (final org.ektorp.DocumentNotFoundException e) {
            throw new DocumentNotFoundException(id, "unknown", e);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public Document read(final String id, final String revision) throws DocumentNotFoundException {
        try {
            final Map<String, String> map = db.get(Map.class, id, revision);
            return asDocument(map);
        } catch (final org.ektorp.DocumentNotFoundException e) {
            throw new DocumentNotFoundException(id, revision, e);
        }
    }

    @Override
    public Document create(final String id, final String json) {
        final Map<String, String> map = asMap(id, null, json);
        db.create(map);
        return asDocument(map);
    }

    @Override
    public Document update(final Document update) throws ConflictException {
        try {
            final Map<String, String> map = asMap(update.id(), update.revision(), update.content());
            db.update(map);
            return asDocument(map);
        } catch (final UpdateConflictException e) {
            final Map<String, String> latest = db.get(Map.class, update.id());
            throw new ConflictException(update, asDocument(latest), e);
        }
    }

    @Override
    public String delete(final String id, final String revision) {
        final String deletedRevision = db.delete(id, revision);
        return deletedRevision;
    }

    private static Document asDocument(final Map<String, String> map) {
        return new DefaultDocument(map.get("_id"), map.get("_rev"), map.get("content"));
    }

    private static Map<String, String> asMap(final String id, final String revision, final String content) {
        final HashMap<String, String> map = new HashMap<String, String>();
        map.put("_id", id);
        if (revision != null) {
            map.put("_rev", revision);
        }
        map.put("content", content);
        return map;
    }

}
