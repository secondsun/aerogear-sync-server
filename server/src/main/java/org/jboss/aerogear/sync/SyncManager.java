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

/**
 * Is responsible handling synchronization of documents.
 */
public interface SyncManager {

    Document read(String id, String revision) throws DocumentNotFoundException;
    Document read(String id) throws DocumentNotFoundException;

    Document create(String id, String json);

    Document update(Document doc) throws ConflictException;

    String delete(String id, String revision);

}
