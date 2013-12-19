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

public class DefaultDocument implements Document {

    private final String id;
    private final String revision;
    private final String content;

    public DefaultDocument(final String id,
                           final String revision,
                           final String content) {
        this.id = id;
        this.revision = revision;
        this.content = content;
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public String revision() {
        return revision;
    }

    @Override
    public String content() {
        return content;
    }

    @Override
    public String toString() {
        return "DefaultDocument[id=" + id + ", revision=" + revision + ", content=" + content + "]";
    }

}
