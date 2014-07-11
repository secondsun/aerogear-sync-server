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
package org.jboss.aerogear.diffsync;

import java.util.LinkedList;

public class DefaultEdit implements Edit {

    private final String clientId;
    private final String documentId;
    private final long clientVersion;
    private final long serverVersion;
    private final String checksum;
    private final LinkedList<Diff> diffs;

    public DefaultEdit(final String documentId,
                       final String clientId,
                       final long clientVersion,
                       final long serverVersion,
                       final String checksum,
                       final LinkedList<Diff> diffs) {
        this.clientId = clientId;
        this.documentId = documentId;
        this.clientVersion = clientVersion;
        this.serverVersion = serverVersion;
        this.checksum = checksum;
        this.diffs = diffs;
    }

    @Override
    public String clientId() {
        return clientId;
    }

    @Override
    public String documentId() {
        return documentId;
    }

    @Override
    public long clientVersion() {
        return clientVersion;
    }

    @Override
    public long serverVersion() {
        return serverVersion;
    }

    @Override
    public String checksum() {
        return checksum;
    }

    @Override
    public LinkedList<Diff> diffs() {
        return diffs;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final DefaultEdit that = (DefaultEdit) o;

        if (clientVersion != that.clientVersion) {
            return false;
        }
        if (serverVersion != that.serverVersion) {
            return false;
        }
        if (!checksum.equals(that.checksum)) {
            return false;
        }
        if (!clientId.equals(that.clientId)) {
            return false;
        }
        if (!diffs.equals(that.diffs)) {
            return false;
        }
        return !documentId.equals(that.documentId);
    }

    @Override
    public int hashCode() {
        int result = clientId.hashCode();
        result = 31 * result + documentId.hashCode();
        result = 31 * result + (int) (clientVersion ^ clientVersion >>> 32);
        result = 31 * result + (int) (serverVersion ^ serverVersion >>> 32);
        result = 31 * result + checksum.hashCode();
        result = 31 * result + diffs.hashCode();
        return result;
    }
}
