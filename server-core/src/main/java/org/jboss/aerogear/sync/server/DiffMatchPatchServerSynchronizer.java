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
package org.jboss.aerogear.sync.server;

import org.jboss.aerogear.sync.*;
import org.jboss.aerogear.sync.common.DiffMatchPatch;

import java.util.LinkedList;
import java.util.Queue;

import static org.jboss.aerogear.sync.common.DiffMatchPatch.Patch;
import static org.jboss.aerogear.sync.common.DiffMatchPatch.builder;
import static org.jboss.aerogear.sync.common.DiffMatchPatch.checksum;

/**
 * A {@link ServerSynchronizer} implementation that can handle text documents.
 */
public class DiffMatchPatchServerSynchronizer implements ServerSynchronizer<String, DefaultEdit> {

    private final DiffMatchPatch diffMatchPatch;

    public DiffMatchPatchServerSynchronizer() {
        this(builder().build());
    }

    public DiffMatchPatchServerSynchronizer(final DiffMatchPatch diffMatchPatch) {
        this.diffMatchPatch = diffMatchPatch;
    }

    @Override
    public DefaultEdit clientDiff(final Document<String> document, final ShadowDocument<String> shadowDocument) {
        final String shadowText = shadowDocument.document().content();
        final LinkedList<DiffMatchPatch.Diff> diffs = diffMatchPatch.diffMain(document.content(), shadowText);
        return DefaultEdit.withDocumentId(document.id())
                .clientId(shadowDocument.document().clientId())
                .checksum(checksum(shadowText))
                .diffs(asAeroGearDiffs(diffs))
                .build();
    }

    @Override
    public DefaultEdit serverDiff(final Document<String> document, final ShadowDocument<String> shadowDocument) {
        final String shadowText = shadowDocument.document().content();
        final LinkedList<DiffMatchPatch.Diff> diffs = diffMatchPatch.diffMain(shadowText, document.content());
        return DefaultEdit.withDocumentId(document.id())
                .clientId(shadowDocument.document().clientId())
                .serverVersion(shadowDocument.serverVersion())
                .clientVersion(shadowDocument.clientVersion())
                .checksum(checksum(shadowText))
                .diffs(asAeroGearDiffs(diffs))
                .build();
    }

    @Override
    public ShadowDocument<String> patchShadow(final DefaultEdit edit, final ShadowDocument<String> shadowDocument) {
        final LinkedList<Patch> patches = patchesFrom(edit);
        final ClientDocument<String> doc = shadowDocument.document();
        //TODO: results also contains a boolean array. Not sure what we should do with it.
        final Object[] results = diffMatchPatch.patchApply(patches, doc.content());
        final ClientDocument<String> patchedDocument = new DefaultClientDocument<String>(doc.id(), doc.clientId(), (String) results[0]);
        return new DefaultShadowDocument<String>(shadowDocument.serverVersion(), edit.clientVersion(), patchedDocument);
    }

    @Override
    public Document<String> patchDocument(final DefaultEdit edit, final Document<String> document) {
        final LinkedList<Patch> patches = patchesFrom(edit);
        final Object[] results = diffMatchPatch.patchApply(patches, document.content());
        //TODO: results also contains a boolean array. Not sure what we should do with it.
        return new DefaultDocument<String>(document.id(), (String) results[0]);
    }

    @Override
    public PatchMessage<DefaultEdit> createPatchMessage(final String documentId,
                                                        final String clientId,
                                                        final Queue<DefaultEdit> edits) {
        return new DefaultPatchMessage(documentId, clientId, edits);
    }

    @Override
    public PatchMessage<DefaultEdit> patchMessageFromJson(String json) {
        return JsonMapper.fromJson(json, DefaultPatchMessage.class);
    }

    private LinkedList<Patch> patchesFrom(final DefaultEdit edit) {
        return diffMatchPatch.patchMake(asDiffUtilDiffs(edit.diffs()));
    }

    private static LinkedList<DiffMatchPatch.Diff> asDiffUtilDiffs(final LinkedList<DiffMatchPatchDiff> diffs) {
        final LinkedList<DiffMatchPatch.Diff> dsf = new LinkedList<DiffMatchPatch.Diff>();
        for (DiffMatchPatchDiff d : diffs) {
            dsf.add(DiffMatchPatch.diff(diffutilOp(d.operation()), d.text()));
        }
        return dsf;
    }

    private static LinkedList<DiffMatchPatchDiff> asAeroGearDiffs(final LinkedList<DiffMatchPatch.Diff> diffs) {
        final LinkedList<DiffMatchPatchDiff> syncDiffs = new LinkedList<DiffMatchPatchDiff>();
        for (DiffMatchPatch.Diff diff : diffs) {
            syncDiffs.add(new DiffMatchPatchDiff(aerogearOp(diff.operation), diff.text));
        }
        return syncDiffs;
    }

    private static DiffMatchPatch.Operation diffutilOp(final DiffMatchPatchDiff.Operation operation) {
        switch (operation) {
            case DELETE:
                return DiffMatchPatch.Operation.DELETE;
            case ADD:
                return DiffMatchPatch.Operation.INSERT;
            case UNCHANGED:
                return DiffMatchPatch.Operation.EQUAL;
            default:
                throw new RuntimeException("Unsupported Operation: " + operation);
        }
    }

    private static DiffMatchPatchDiff.Operation aerogearOp(final DiffMatchPatch.Operation operation) {
        switch (operation) {
            case DELETE:
                return DiffMatchPatchDiff.Operation.DELETE;
            case INSERT:
                return DiffMatchPatchDiff.Operation.ADD;
            case EQUAL:
                return DiffMatchPatchDiff.Operation.UNCHANGED;
            default:
                throw new RuntimeException("Unsupported Operation: " + operation);
        }
    }

}
