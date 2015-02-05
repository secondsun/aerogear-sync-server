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
package org.jboss.aerogear.sync.diffmatchpatch;

import org.jboss.aerogear.sync.Diff;
import org.jboss.aerogear.sync.util.Arguments;

import java.util.LinkedList;

public class DiffMatchPatchDiffs implements Diff {

    private final LinkedList<DiffMatchPatchDiff> diffs;

    public DiffMatchPatchDiffs(final LinkedList<DiffMatchPatchDiff> diffs) {
        this.diffs = Arguments.checkNotNull(diffs, "diffs must not be null");
    }

    public LinkedList<DiffMatchPatchDiff> diffs() {
        return diffs;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final DiffMatchPatchDiffs that = (DiffMatchPatchDiffs) o;
        if (diffs == that.diffs) {
            return true;
        }
        return diffs.equals(that.diffs);
    }

    @Override
    public int hashCode() {
        return diffs != null ? diffs.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "DiffMatchPatchDiffs[diffs=" + diffs +  ']';
    }
}
