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


import org.jboss.aerogear.sync.diffmatchpatch.DiffMatchPatchDiff.Operation;
import org.junit.Test;

import java.util.Collections;
import java.util.LinkedList;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class DiffMatchPatchDiffsTest {

    @Test (expected = NullPointerException.class)
    public void constructWithNullDiffs() {
        new DiffMatchPatchDiffs(null);
    }

    @Test
    public void equalsReflexsive() {
        final DiffMatchPatchDiffs x = new DiffMatchPatchDiffs(asQueue(new DiffMatchPatchDiff(Operation.ADD, "Fletch")));
        assertThat(x, equalTo(x));
    }

    @Test
    public void equalsSymmetric() {
        final DiffMatchPatchDiffs x = new DiffMatchPatchDiffs(asQueue(new DiffMatchPatchDiff(Operation.ADD, "Fletch")));
        final DiffMatchPatchDiffs y = new DiffMatchPatchDiffs(asQueue(new DiffMatchPatchDiff(Operation.ADD, "Fletch")));
        assertThat(x, equalTo(y));
        assertThat(y, equalTo(x));
    }

    @Test
    public void equalsTransitive() {
        final DiffMatchPatchDiffs x = new DiffMatchPatchDiffs(asQueue(new DiffMatchPatchDiff(Operation.ADD, "Fletch")));
        final DiffMatchPatchDiffs y = new DiffMatchPatchDiffs(asQueue(new DiffMatchPatchDiff(Operation.ADD, "Fletch")));
        final DiffMatchPatchDiffs z = new DiffMatchPatchDiffs(asQueue(new DiffMatchPatchDiff(Operation.ADD, "Fletch")));
        assertThat(x, equalTo(y));
        assertThat(y, equalTo(z));
        assertThat(x, equalTo(z));
    }

    @Test
    public void equalsNull() {
        final DiffMatchPatchDiffs x = new DiffMatchPatchDiffs(asQueue(new DiffMatchPatchDiff(Operation.ADD, "Fletch")));
        assertThat(x.equals(null), is(false));
    }

    @Test
    public void noEquals() {
        final DiffMatchPatchDiffs x = new DiffMatchPatchDiffs(asQueue(new DiffMatchPatchDiff(Operation.ADD, "Fletch")));
        final DiffMatchPatchDiffs y = new DiffMatchPatchDiffs(asQueue(new DiffMatchPatchDiff(Operation.ADD, "fletch")));
        assertThat(x.equals(y), is(false));
    }

    private static LinkedList<DiffMatchPatchDiff> asQueue(final DiffMatchPatchDiff diff) {
        return new LinkedList<DiffMatchPatchDiff>(Collections.singleton(diff));
    }

}