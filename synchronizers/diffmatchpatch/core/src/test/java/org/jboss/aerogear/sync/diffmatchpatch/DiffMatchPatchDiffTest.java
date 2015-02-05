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

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class DiffMatchPatchDiffTest {

    @Test (expected = NullPointerException.class)
    public void constructWithNullOperation() {
        new DiffMatchPatchDiff(null, "Fletch");
    }

    @Test (expected = NullPointerException.class)
    public void constructWithNullText() {
        new DiffMatchPatchDiff(Operation.ADD, null);
    }

    @Test
    public void equalsReflexsive() {
        final DiffMatchPatchDiff x = new DiffMatchPatchDiff(Operation.ADD, "Fletch");
        assertThat(x, equalTo(x));
    }

    @Test
    public void equalsSymmetric() {
        final DiffMatchPatchDiff x = new DiffMatchPatchDiff(Operation.ADD, "Fletch");
        final DiffMatchPatchDiff y = new DiffMatchPatchDiff(Operation.ADD, "Fletch");
        assertThat(x, equalTo(y));
        assertThat(y, equalTo(x));
    }

    @Test
    public void equalsTransitive() {
        final DiffMatchPatchDiff x = new DiffMatchPatchDiff(Operation.ADD, "Fletch");
        final DiffMatchPatchDiff y = new DiffMatchPatchDiff(Operation.ADD, "Fletch");
        final DiffMatchPatchDiff z = new DiffMatchPatchDiff(Operation.ADD, "Fletch");
        assertThat(x, equalTo(y));
        assertThat(y, equalTo(z));
        assertThat(x, equalTo(z));
    }

    @Test
    public void equalsNull() {
        final DiffMatchPatchDiff x = new DiffMatchPatchDiff(Operation.ADD, "Fletch");
        assertThat(x.equals(null), is(false));
    }

    @Test
    public void noEquals() {
        final DiffMatchPatchDiff x = new DiffMatchPatchDiff(Operation.ADD, "Fletch");
        final DiffMatchPatchDiff y = new DiffMatchPatchDiff(Operation.DELETE, "Fletch");
        assertThat(x.equals(y), is(false));
    }

}