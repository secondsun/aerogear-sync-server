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
    public Edit diff(final Document<String> document, final ShadowDocument<String> shadowDocument) {
        final String shadowText = shadowDocument.document().content();
        final LinkedList<DiffUtil.Diff> diffs = diffUtil.diffMain(shadowText, document.content());
        return new DefaultEdit(shadowDocument.clientVersion(), checksum(shadowText), asAeroGearDiffs(diffs));
    }

    @Override
    public ShadowDocument<String> patchShadow(final Edit edit, final ShadowDocument<String> shadowDocument) {
        final LinkedList<Patch> patches = diffUtil.patchMake(asDiffUtilDiffs(edit.diffs()));
        final Object[] results = diffUtil.patchApply(patches, shadowDocument.document().content());
        final String patchedText = (String) results[0];
        //TODO: results also contains a boolean array. Not sure what we should do with it.
        final boolean[] bx = (boolean[]) results[1];
        return new DefaultShadowDocument<String>(shadowDocument.serverVersion(),
                edit.version(),
                new DefaultDocument<String>(patchedText));
    }

    @Override
    public Document<String> patchDocument(final Edit edit, final Document<String> document) {
        return null;
    }

    private static LinkedList<DiffUtil.Diff> asDiffUtilDiffs(final LinkedList<Diff> diffs) {
        final LinkedList<DiffUtil.Diff> dsf = new LinkedList<DiffUtil.Diff>();
        for (Diff d : diffs) {
            dsf.add(new DiffUtil.Diff(diffutilOp(d.operation()), d.text()));
        }
        return dsf;
    }

    private static LinkedList<Diff> asAeroGearDiffs(final LinkedList<DiffUtil.Diff> diffs) {
        final LinkedList<Diff> syncDiffs = new LinkedList<Diff>();
        for (DiffUtil.Diff diff : diffs) {
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
