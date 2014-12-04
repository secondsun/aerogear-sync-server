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
package org.jboss.aerogear.sync.client;

import org.jboss.aerogear.sync.*;
import org.jboss.aerogear.sync.Diff;
import org.jboss.aerogear.sync.common.DiffMatchPatch;

import java.util.LinkedList;

import static org.jboss.aerogear.sync.common.DiffMatchPatch.*;

/**
 * A {@link ClientSynchronizer} implementation that can handle text documents.
 */
public class DefaultClientSynchronizer implements ClientSynchronizer<String> {

    private final DiffMatchPatch diffMatchPatch;

    public DefaultClientSynchronizer() {
        this(builder().build());
    }

    public DefaultClientSynchronizer(final DiffMatchPatch diffMatchPatch) {
        this.diffMatchPatch = diffMatchPatch;
    }

    @Override
    public Edit clientDiff(final Document<String> document, final ShadowDocument<String> shadowDocument) {
        final String shadowText = shadowDocument.document().content();
        final LinkedList<DiffMatchPatch.Diff> diffs = diffMatchPatch.diffMain(document.content(), shadowText);
        return DefaultEdit.withDocumentId(document.id())
                .clientId(shadowDocument.document().clientId())
                .clientVersion(shadowDocument.clientVersion())
                .serverVersion(shadowDocument.serverVersion())
                .checksum(checksum(shadowText))
                .diffs(asAeroGearDiffs(diffs))
                .build();
    }
    
    @Override
    public Edit serverDiff(final Document<String> document, final ShadowDocument<String> shadowDocument) {
        final String shadowText = shadowDocument.document().content();
        final LinkedList<DiffMatchPatch.Diff> diffs = diffMatchPatch.diffMain(shadowText, document.content());
        return DefaultEdit.withDocumentId(document.id())
                .clientId(shadowDocument.document().clientId())
                .clientVersion(shadowDocument.clientVersion())
                .serverVersion(shadowDocument.serverVersion())
                .checksum(checksum(shadowText))
                .diffs(asAeroGearDiffs(diffs))
                .build();
    }

    @Override
    public ShadowDocument<String> patchShadow(final Edit edit, final ShadowDocument<String> shadowDocument) {
        final LinkedList<Patch> patches = patchesFrom(edit);
        final ClientDocument<String> doc = shadowDocument.document();
        //TODO: results also contains a boolean array. Not sure what we should do with it.
        final Object[] results = diffMatchPatch.patchApply(patches, doc.content());
        final ClientDocument<String> patchedDocument = new DefaultClientDocument<String>(doc.id(), doc.clientId(), (String) results[0]);
        return new DefaultShadowDocument<String>(shadowDocument.serverVersion(), edit.clientVersion(), patchedDocument);
    }

    @Override
    public ClientDocument<String> patchDocument(final Edit edit, final ClientDocument<String> document) {
        final LinkedList<Patch> patches = patchesFrom(edit);
        //TODO: results also contains a boolean array. Not sure what we should do with it.
        final Object[] results = diffMatchPatch.patchApply(patches, document.content());
        return new DefaultClientDocument<String>(document.id(), document.clientId(), (String) results[0]);
    }

    private LinkedList<Patch> patchesFrom(final Edit edit) {
        return diffMatchPatch.patchMake(asDiffUtilDiffs(edit.diffs()));
    }

    private static LinkedList<DiffMatchPatch.Diff> asDiffUtilDiffs(final LinkedList<Diff> diffs) {
        final LinkedList<DiffMatchPatch.Diff> dsf = new LinkedList<DiffMatchPatch.Diff>();
        for (Diff d : diffs) {
            dsf.add(new DiffMatchPatch.Diff(diffutilOp(d.operation()), d.text()));
        }
        return dsf;
    }

    private static LinkedList<Diff> asAeroGearDiffs(final LinkedList<DiffMatchPatch.Diff> diffs) {
        final LinkedList<Diff> syncDiffs = new LinkedList<Diff>();
        for (DiffMatchPatch.Diff diff : diffs) {
            syncDiffs.add(new DefaultDiff(aerogearOp(diff.operation), diff.text));
        }
        return syncDiffs;
    }

    private static Operation diffutilOp(final Diff.Operation operation) {
        switch (operation) {
            case DELETE:
                return Operation.DELETE;
            case ADD:
                return Operation.INSERT;
            case UNCHANGED:
                return Operation.EQUAL;
            default:
                throw new RuntimeException("Unsupported Operation: " + operation);
        }
    }

    private static Diff.Operation aerogearOp(final Operation operation) {
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
