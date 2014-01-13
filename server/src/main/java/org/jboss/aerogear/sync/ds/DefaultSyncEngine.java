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

import org.jboss.aerogear.sync.common.DiffUtil;

import java.math.BigInteger;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.util.LinkedList;

import static org.jboss.aerogear.sync.common.DiffUtil.*;

/**
 *
 */
public class DefaultSyncEngine implements SyncEngine<String> {

    private final DiffUtil diffUtil;

    public DefaultSyncEngine() {
        this(builder().build());
    }

    public DefaultSyncEngine(final DiffUtil diffUtil) {
        this.diffUtil = diffUtil;
    }

    @Override
    public Document<String> patchShadow(final Edit edit) {
        return null;
    }

    @Override
    public Document<String> patchDocument(final Edit edit) {
        return null;
    }

    @Override
    public Edit diff(final Document<String> document, final ShadowDocument<String> shadowDocument) {
        final String shadowText = shadowDocument.document().content();
        final LinkedList<DiffUtil.Diff> diffs = diffUtil.diffMain(shadowText, document.content());
        return new DefaultEdit(shadowDocument.clientVersion(), checksum(shadowText), asAeroGearDiffs(diffs));
    }

    private static LinkedList<Diff> asAeroGearDiffs(final LinkedList<DiffUtil.Diff> diffs) {
        final LinkedList<Diff> syncDiffs = new LinkedList<Diff>();
        for (DiffUtil.Diff diff : diffs) {
            syncDiffs.add(new DefaultDiff(translateOp(diff.operation), diff.text));
        }
        return syncDiffs;
    }

    private static Diff.Operation translateOp(final Operation operation) {
        switch (operation) {
            case DELETE:
                return Diff.Operation.DELETE;
            case INSERT:
                return Diff.Operation.ADD;
            case EQUAL:
                return Diff.Operation.UNCHANGED;
            default:
                throw new RuntimeException("Unsupported Operation: " + operation);
        }
    }

}
