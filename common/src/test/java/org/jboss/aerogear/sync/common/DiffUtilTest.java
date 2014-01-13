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
package org.jboss.aerogear.sync.common;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static junit.framework.Assert.assertTrue;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class DiffUtilTest {

    private static final DiffUtil.Operation DELETE = DiffUtil.Operation.DELETE;
    private static final DiffUtil.Operation EQUAL = DiffUtil.Operation.EQUAL;
    private static final DiffUtil.Operation INSERT = DiffUtil.Operation.INSERT;
    private DiffUtil diffUtil;

    @Before
    public void createDiffUtilInstance() {
        diffUtil = new DiffUtil.Builder().build();
    }

    @Test
    public void diffCommonPrefix() {
        assertThat(diffUtil.diffCommonPrefix("abc", "xyz"), is(0));
        assertThat(diffUtil.diffCommonPrefix("1234abcdef", "1234xyz"), is(4));
        assertThat(diffUtil.diffCommonPrefix("1234", "1234xyz"), is(4));
    }

    @Test
    public void diffCommonSuffix() {
        assertThat(diffUtil.diffCommonSuffix("abc", "xyz"), is(0));
        assertThat(diffUtil.diffCommonSuffix("abcdef1234", "xyz1234"), is(4));
        assertThat(diffUtil.diffCommonSuffix("1234", "xyz1234"), is(4));
    }

    @Test
    public void diffCommonOverlap() {
        assertThat(diffUtil.diffCommonOverlap("", "abcd"), is(0));
        assertThat(diffUtil.diffCommonOverlap("abc", "abcd"), is(3));
        assertThat(diffUtil.diffCommonOverlap("123456", "abcd"), is(0));
        assertThat(diffUtil.diffCommonOverlap("123456xxx", "xxxabcd"), is(3));

        // Some overly clever languages (C#) may treat ligatures as equal to their
        // component letters.  E.g. U+FB01 == 'fi'
        assertThat(diffUtil.diffCommonOverlap("fi", "\ufb01i"), is(0));
    }

    @Test
    public void diffHalfmatch() {
        DiffUtil diffUtil = DiffUtil.builder().patchTimeout(1).build();
        assertThat(diffUtil.diffHalfMatch("1234567890", "abcdef"), is(nullValue()));
        assertThat(diffUtil.diffHalfMatch("12345", "23"), is(nullValue()));
        assertThat(diffUtil.diffHalfMatch("1234567890", "a345678z"), equalTo(new String[]{"12", "90", "a", "z", "345678"}));
        assertThat(diffUtil.diffHalfMatch("a345678z", "1234567890"), equalTo(new String[]{"a", "z", "12", "90", "345678"}));
        assertThat(diffUtil.diffHalfMatch("abc56789z", "1234567890"), equalTo(new String[]{"abc", "z", "1234", "0", "56789"}));
        assertThat(diffUtil.diffHalfMatch("a23456xyz", "1234567890"), equalTo(new String[]{"a", "xyz", "1", "7890", "23456"}));
        assertThat(diffUtil.diffHalfMatch("121231234123451234123121", "a1234123451234z"), equalTo(new String[]{"12123", "123121", "a", "z", "1234123451234"}));
        assertThat(diffUtil.diffHalfMatch("x-=-=-=-=-=-=-=-=-=-=-=-=", "xx-=-=-=-=-=-=-="), equalTo(new String[]{"", "-=-=-=-=-=", "x", "", "x-=-=-=-=-=-=-="}));
        assertThat(diffUtil.diffHalfMatch("-=-=-=-=-=-=-=-=-=-=-=-=y", "-=-=-=-=-=-=-=yy"), equalTo(new String[]{"-=-=-=-=-=", "", "", "y", "-=-=-=-=-=-=-=y"}));
        assertThat(diffUtil.diffHalfMatch("qHilloHelloHew", "xHelloHeHulloy"), equalTo(new String[]{"qHillo", "w", "x", "Hulloy", "HelloHe"}));
        diffUtil = DiffUtil.builder().patchTimeout(0).build();
        assertThat(diffUtil.diffHalfMatch("qHilloHelloHew", "xHelloHeHulloy"), is(nullValue()));
    }

    @Test
    public void diffLinesToChars() {
        // Convert lines down to characters.
        List<String> tmpVector = new ArrayList<String>();
        tmpVector.add("");
        tmpVector.add("alpha\n");
        tmpVector.add("beta\n");
        assertLinesToCharsResultEquals("diffLinesToChars: Shared lines.", new DiffUtil.LinesToCharsResult("\u0001\u0002\u0001", "\u0002\u0001\u0002", tmpVector), diffUtil.diffLinesToChars("alpha\nbeta\nalpha\n", "beta\nalpha\nbeta\n"));

        tmpVector.clear();
        tmpVector.add("");
        tmpVector.add("alpha\r\n");
        tmpVector.add("beta\r\n");
        tmpVector.add("\r\n");
        assertLinesToCharsResultEquals("diffLinesToChars: Empty string and blank lines.", new DiffUtil.LinesToCharsResult("", "\u0001\u0002\u0003\u0003", tmpVector), diffUtil.diffLinesToChars("", "alpha\r\nbeta\r\n\r\n\r\n"));

        tmpVector.clear();
        tmpVector.add("");
        tmpVector.add("a");
        tmpVector.add("b");
        assertLinesToCharsResultEquals("diffLinesToChars: No linebreaks.", new DiffUtil.LinesToCharsResult("\u0001", "\u0002", tmpVector), diffUtil.diffLinesToChars("a", "b"));

        // More than 256 to reveal any 8-bit limitations.
        int n = 300;
        tmpVector.clear();
        StringBuilder lineList = new StringBuilder();
        StringBuilder charList = new StringBuilder();
        for (int x = 1; x < n + 1; x++) {
            tmpVector.add(x + "\n");
            lineList.append(x + "\n");
            charList.append((char) x);
        }
        assertEquals(n, tmpVector.size());
        String lines = lineList.toString();
        String chars = charList.toString();
        assertEquals(n, chars.length());
        tmpVector.add(0, "");
        assertLinesToCharsResultEquals("diffLinesToChars: More than 256.", new DiffUtil.LinesToCharsResult(chars, "", tmpVector), diffUtil.diffLinesToChars(lines, ""));
    }

    @Test
    public void testDiffCharsToLines() {
        // First check that Diff equality works.
        assertTrue("diffCharsToLines: Equality #1.", new DiffUtil.Diff(EQUAL, "a").equals(new DiffUtil.Diff(EQUAL, "a")));

        assertEquals("diffCharsToLines: Equality #2.", new DiffUtil.Diff(EQUAL, "a"), new DiffUtil.Diff(EQUAL, "a"));

        // Convert chars up to lines.
        LinkedList<DiffUtil.Diff> diffs = diffList(new DiffUtil.Diff(EQUAL, "\u0001\u0002\u0001"), new DiffUtil.Diff(INSERT, "\u0002\u0001\u0002"));
        List<String> tmpVector = new ArrayList<String>();
        tmpVector.add("");
        tmpVector.add("alpha\n");
        tmpVector.add("beta\n");
        diffUtil.diffCharsToLines(diffs, tmpVector);
        assertEquals("diffCharsToLines: Shared lines.", diffList(new DiffUtil.Diff(EQUAL, "alpha\nbeta\nalpha\n"), new DiffUtil.Diff(INSERT, "beta\nalpha\nbeta\n")), diffs);

        // More than 256 to reveal any 8-bit limitations.
        int n = 300;
        tmpVector.clear();
        StringBuilder lineList = new StringBuilder();
        StringBuilder charList = new StringBuilder();
        for (int x = 1; x < n + 1; x++) {
            tmpVector.add(x + "\n");
            lineList.append(x + "\n");
            charList.append((char) x);
        }
        assertEquals(n, tmpVector.size());
        String lines = lineList.toString();
        String chars = charList.toString();
        assertEquals(n, chars.length());
        tmpVector.add(0, "");
        diffs = diffList(new DiffUtil.Diff(DELETE, chars));
        diffUtil.diffCharsToLines(diffs, tmpVector);
        assertEquals("diffCharsToLines: More than 256.", diffList(new DiffUtil.Diff(DELETE, lines)), diffs);
    }

    @Test
    public void testDiffCleanupMerge() {
        // Cleanup a messy diff.
        LinkedList<DiffUtil.Diff> diffs = diffList();
        diffUtil.diffCleanupMerge(diffs);
        assertEquals("diffCleanupMerge: Null case.", diffList(), diffs);

        diffs = diffList(new DiffUtil.Diff(EQUAL, "a"), new DiffUtil.Diff(DELETE, "b"), new DiffUtil.Diff(INSERT, "c"));
        diffUtil.diffCleanupMerge(diffs);
        assertEquals("diffCleanupMerge: No change case.", diffList(new DiffUtil.Diff(EQUAL, "a"), new DiffUtil.Diff(DELETE, "b"), new DiffUtil.Diff(INSERT, "c")), diffs);

        diffs = diffList(new DiffUtil.Diff(EQUAL, "a"), new DiffUtil.Diff(EQUAL, "b"), new DiffUtil.Diff(EQUAL, "c"));
        diffUtil.diffCleanupMerge(diffs);
        assertEquals("diffCleanupMerge: Merge equalities.", diffList(new DiffUtil.Diff(EQUAL, "abc")), diffs);

        diffs = diffList(new DiffUtil.Diff(DELETE, "a"), new DiffUtil.Diff(DELETE, "b"), new DiffUtil.Diff(DELETE, "c"));
        diffUtil.diffCleanupMerge(diffs);
        assertEquals("diffCleanupMerge: Merge deletions.", diffList(new DiffUtil.Diff(DELETE, "abc")), diffs);

        diffs = diffList(new DiffUtil.Diff(INSERT, "a"), new DiffUtil.Diff(INSERT, "b"), new DiffUtil.Diff(INSERT, "c"));
        diffUtil.diffCleanupMerge(diffs);
        assertEquals("diffCleanupMerge: Merge insertions.", diffList(new DiffUtil.Diff(INSERT, "abc")), diffs);

        diffs = diffList(new DiffUtil.Diff(DELETE, "a"), new DiffUtil.Diff(INSERT, "b"), new DiffUtil.Diff(DELETE, "c"), new DiffUtil.Diff(INSERT, "d"), new DiffUtil.Diff(EQUAL, "e"), new DiffUtil.Diff(EQUAL, "f"));
        diffUtil.diffCleanupMerge(diffs);
        assertEquals("diffCleanupMerge: Merge interweave.", diffList(new DiffUtil.Diff(DELETE, "ac"), new DiffUtil.Diff(INSERT, "bd"), new DiffUtil.Diff(EQUAL, "ef")), diffs);

        diffs = diffList(new DiffUtil.Diff(DELETE, "a"), new DiffUtil.Diff(INSERT, "abc"), new DiffUtil.Diff(DELETE, "dc"));
        diffUtil.diffCleanupMerge(diffs);
        assertEquals("diffCleanupMerge: Prefix and suffix detection.", diffList(new DiffUtil.Diff(EQUAL, "a"), new DiffUtil.Diff(DELETE, "d"), new DiffUtil.Diff(INSERT, "b"), new DiffUtil.Diff(EQUAL, "c")), diffs);

        diffs = diffList(new DiffUtil.Diff(EQUAL, "x"), new DiffUtil.Diff(DELETE, "a"), new DiffUtil.Diff(INSERT, "abc"), new DiffUtil.Diff(DELETE, "dc"), new DiffUtil.Diff(EQUAL, "y"));
        diffUtil.diffCleanupMerge(diffs);
        assertEquals("diffCleanupMerge: Prefix and suffix detection with equalities.", diffList(new DiffUtil.Diff(EQUAL, "xa"), new DiffUtil.Diff(DELETE, "d"), new DiffUtil.Diff(INSERT, "b"), new DiffUtil.Diff(EQUAL, "cy")), diffs);

        diffs = diffList(new DiffUtil.Diff(EQUAL, "a"), new DiffUtil.Diff(INSERT, "ba"), new DiffUtil.Diff(EQUAL, "c"));
        diffUtil.diffCleanupMerge(diffs);
        assertEquals("diffCleanupMerge: Slide edit left.", diffList(new DiffUtil.Diff(INSERT, "ab"), new DiffUtil.Diff(EQUAL, "ac")), diffs);

        diffs = diffList(new DiffUtil.Diff(EQUAL, "c"), new DiffUtil.Diff(INSERT, "ab"), new DiffUtil.Diff(EQUAL, "a"));
        diffUtil.diffCleanupMerge(diffs);
        assertEquals("diffCleanupMerge: Slide edit right.", diffList(new DiffUtil.Diff(EQUAL, "ca"), new DiffUtil.Diff(INSERT, "ba")), diffs);

        diffs = diffList(new DiffUtil.Diff(EQUAL, "a"), new DiffUtil.Diff(DELETE, "b"), new DiffUtil.Diff(EQUAL, "c"), new DiffUtil.Diff(DELETE, "ac"), new DiffUtil.Diff(EQUAL, "x"));
        diffUtil.diffCleanupMerge(diffs);
        assertEquals("diffCleanupMerge: Slide edit left recursive.", diffList(new DiffUtil.Diff(DELETE, "abc"), new DiffUtil.Diff(EQUAL, "acx")), diffs);

        diffs = diffList(new DiffUtil.Diff(EQUAL, "x"), new DiffUtil.Diff(DELETE, "ca"), new DiffUtil.Diff(EQUAL, "c"), new DiffUtil.Diff(DELETE, "b"), new DiffUtil.Diff(EQUAL, "a"));
        diffUtil.diffCleanupMerge(diffs);
        assertEquals("diffCleanupMerge: Slide edit right recursive.", diffList(new DiffUtil.Diff(EQUAL, "xca"), new DiffUtil.Diff(DELETE, "cba")), diffs);
    }

    @Test
    public void testDiffCleanupSemanticLossless() {
        // Slide diffs to match logical boundaries.
        LinkedList<DiffUtil.Diff> diffs = diffList();
        diffUtil.diffCleanupSemanticLossLess(diffs);
        assertEquals("diffCleanupSemanticLossLess: Null case.", diffList(), diffs);

        diffs = diffList(new DiffUtil.Diff(EQUAL, "AAA\r\n\r\nBBB"), new DiffUtil.Diff(INSERT, "\r\nDDD\r\n\r\nBBB"), new DiffUtil.Diff(EQUAL, "\r\nEEE"));
        diffUtil.diffCleanupSemanticLossLess(diffs);
        assertEquals("diffCleanupSemanticLossLess: Blank lines.", diffList(new DiffUtil.Diff(EQUAL, "AAA\r\n\r\n"), new DiffUtil.Diff(INSERT, "BBB\r\nDDD\r\n\r\n"), new DiffUtil.Diff(EQUAL, "BBB\r\nEEE")), diffs);

        diffs = diffList(new DiffUtil.Diff(EQUAL, "AAA\r\nBBB"), new DiffUtil.Diff(INSERT, " DDD\r\nBBB"), new DiffUtil.Diff(EQUAL, " EEE"));
        diffUtil.diffCleanupSemanticLossLess(diffs);
        assertEquals("diffCleanupSemanticLossLess: Line boundaries.", diffList(new DiffUtil.Diff(EQUAL, "AAA\r\n"), new DiffUtil.Diff(INSERT, "BBB DDD\r\n"), new DiffUtil.Diff(EQUAL, "BBB EEE")), diffs);

        diffs = diffList(new DiffUtil.Diff(EQUAL, "The c"), new DiffUtil.Diff(INSERT, "ow and the c"), new DiffUtil.Diff(EQUAL, "at."));
        diffUtil.diffCleanupSemanticLossLess(diffs);
        assertEquals("diffCleanupSemanticLossLess: Word boundaries.", diffList(new DiffUtil.Diff(EQUAL, "The "), new DiffUtil.Diff(INSERT, "cow and the "), new DiffUtil.Diff(EQUAL, "cat.")), diffs);

        diffs = diffList(new DiffUtil.Diff(EQUAL, "The-c"), new DiffUtil.Diff(INSERT, "ow-and-the-c"), new DiffUtil.Diff(EQUAL, "at."));
        diffUtil.diffCleanupSemanticLossLess(diffs);
        assertEquals("diffCleanupSemanticLossLess: Alphanumeric boundaries.", diffList(new DiffUtil.Diff(EQUAL, "The-"), new DiffUtil.Diff(INSERT, "cow-and-the-"), new DiffUtil.Diff(EQUAL, "cat.")), diffs);

        diffs = diffList(new DiffUtil.Diff(EQUAL, "a"), new DiffUtil.Diff(DELETE, "a"), new DiffUtil.Diff(EQUAL, "ax"));
        diffUtil.diffCleanupSemanticLossLess(diffs);
        assertEquals("diffCleanupSemanticLossLess: Hitting the start.", diffList(new DiffUtil.Diff(DELETE, "a"), new DiffUtil.Diff(EQUAL, "aax")), diffs);

        diffs = diffList(new DiffUtil.Diff(EQUAL, "xa"), new DiffUtil.Diff(DELETE, "a"), new DiffUtil.Diff(EQUAL, "a"));
        diffUtil.diffCleanupSemanticLossLess(diffs);
        assertEquals("diffCleanupSemanticLossLess: Hitting the end.", diffList(new DiffUtil.Diff(EQUAL, "xaa"), new DiffUtil.Diff(DELETE, "a")), diffs);

        diffs = diffList(new DiffUtil.Diff(EQUAL, "The xxx. The "), new DiffUtil.Diff(INSERT, "zzz. The "), new DiffUtil.Diff(EQUAL, "yyy."));
        diffUtil.diffCleanupSemanticLossLess(diffs);
        assertEquals("diffCleanupSemanticLossLess: Sentence boundaries.", diffList(new DiffUtil.Diff(EQUAL, "The xxx."), new DiffUtil.Diff(INSERT, " The zzz."), new DiffUtil.Diff(EQUAL, " The yyy.")), diffs);
    }

    @Test
    public void testDiffCleanupSemantic() {
        // Cleanup semantically trivial equalities.
        LinkedList<DiffUtil.Diff> diffs = diffList();
        diffUtil.diff_cleanupSemantic(diffs);
        assertEquals("diff_cleanupSemantic: Null case.", diffList(), diffs);

        diffs = diffList(new DiffUtil.Diff(DELETE, "ab"), new DiffUtil.Diff(INSERT, "cd"), new DiffUtil.Diff(EQUAL, "12"), new DiffUtil.Diff(DELETE, "e"));
        diffUtil.diff_cleanupSemantic(diffs);
        assertEquals("diff_cleanupSemantic: No elimination #1.", diffList(new DiffUtil.Diff(DELETE, "ab"), new DiffUtil.Diff(INSERT, "cd"), new DiffUtil.Diff(EQUAL, "12"), new DiffUtil.Diff(DELETE, "e")), diffs);

        diffs = diffList(new DiffUtil.Diff(DELETE, "abc"), new DiffUtil.Diff(INSERT, "ABC"), new DiffUtil.Diff(EQUAL, "1234"), new DiffUtil.Diff(DELETE, "wxyz"));
        diffUtil.diff_cleanupSemantic(diffs);
        assertEquals("diff_cleanupSemantic: No elimination #2.", diffList(new DiffUtil.Diff(DELETE, "abc"), new DiffUtil.Diff(INSERT, "ABC"), new DiffUtil.Diff(EQUAL, "1234"), new DiffUtil.Diff(DELETE, "wxyz")), diffs);

        diffs = diffList(new DiffUtil.Diff(DELETE, "a"), new DiffUtil.Diff(EQUAL, "b"), new DiffUtil.Diff(DELETE, "c"));
        diffUtil.diff_cleanupSemantic(diffs);
        assertEquals("diff_cleanupSemantic: Simple elimination.", diffList(new DiffUtil.Diff(DELETE, "abc"), new DiffUtil.Diff(INSERT, "b")), diffs);

        diffs = diffList(new DiffUtil.Diff(DELETE, "ab"), new DiffUtil.Diff(EQUAL, "cd"), new DiffUtil.Diff(DELETE, "e"), new DiffUtil.Diff(EQUAL, "f"), new DiffUtil.Diff(INSERT, "g"));
        diffUtil.diff_cleanupSemantic(diffs);
        assertEquals("diff_cleanupSemantic: Backpass elimination.", diffList(new DiffUtil.Diff(DELETE, "abcdef"), new DiffUtil.Diff(INSERT, "cdfg")), diffs);

        diffs = diffList(new DiffUtil.Diff(INSERT, "1"), new DiffUtil.Diff(EQUAL, "A"), new DiffUtil.Diff(DELETE, "B"), new DiffUtil.Diff(INSERT, "2"), new DiffUtil.Diff(EQUAL, "_"), new DiffUtil.Diff(INSERT, "1"), new DiffUtil.Diff(EQUAL, "A"), new DiffUtil.Diff(DELETE, "B"), new DiffUtil.Diff(INSERT, "2"));
        diffUtil.diff_cleanupSemantic(diffs);
        assertEquals("diff_cleanupSemantic: Multiple elimination.", diffList(new DiffUtil.Diff(DELETE, "AB_AB"), new DiffUtil.Diff(INSERT, "1A2_1A2")), diffs);

        diffs = diffList(new DiffUtil.Diff(EQUAL, "The c"), new DiffUtil.Diff(DELETE, "ow and the c"), new DiffUtil.Diff(EQUAL, "at."));
        diffUtil.diff_cleanupSemantic(diffs);
        assertEquals("diff_cleanupSemantic: Word boundaries.", diffList(new DiffUtil.Diff(EQUAL, "The "), new DiffUtil.Diff(DELETE, "cow and the "), new DiffUtil.Diff(EQUAL, "cat.")), diffs);

        diffs = diffList(new DiffUtil.Diff(DELETE, "abcxx"), new DiffUtil.Diff(INSERT, "xxdef"));
        diffUtil.diff_cleanupSemantic(diffs);
        assertEquals("diff_cleanupSemantic: No overlap elimination.", diffList(new DiffUtil.Diff(DELETE, "abcxx"), new DiffUtil.Diff(INSERT, "xxdef")), diffs);

        diffs = diffList(new DiffUtil.Diff(DELETE, "abcxxx"), new DiffUtil.Diff(INSERT, "xxxdef"));
        diffUtil.diff_cleanupSemantic(diffs);
        assertEquals("diff_cleanupSemantic: Overlap elimination.", diffList(new DiffUtil.Diff(DELETE, "abc"), new DiffUtil.Diff(EQUAL, "xxx"), new DiffUtil.Diff(INSERT, "def")), diffs);

        diffs = diffList(new DiffUtil.Diff(DELETE, "xxxabc"), new DiffUtil.Diff(INSERT, "defxxx"));
        diffUtil.diff_cleanupSemantic(diffs);
        assertEquals("diff_cleanupSemantic: Reverse overlap elimination.", diffList(new DiffUtil.Diff(INSERT, "def"), new DiffUtil.Diff(EQUAL, "xxx"), new DiffUtil.Diff(DELETE, "abc")), diffs);

        diffs = diffList(new DiffUtil.Diff(DELETE, "abcd1212"), new DiffUtil.Diff(INSERT, "1212efghi"), new DiffUtil.Diff(EQUAL, "----"), new DiffUtil.Diff(DELETE, "A3"), new DiffUtil.Diff(INSERT, "3BC"));
        diffUtil.diff_cleanupSemantic(diffs);
        assertEquals("diff_cleanupSemantic: Two overlap eliminations.", diffList(new DiffUtil.Diff(DELETE, "abcd"), new DiffUtil.Diff(EQUAL, "1212"), new DiffUtil.Diff(INSERT, "efghi"), new DiffUtil.Diff(EQUAL, "----"), new DiffUtil.Diff(DELETE, "A"), new DiffUtil.Diff(EQUAL, "3"), new DiffUtil.Diff(INSERT, "BC")), diffs);
    }

    @Test
    public void testDiffCleanupEfficiency() {
        // Cleanup operationally trivial equalities.
        diffUtil = DiffUtil.builder().diffEditCost(4).build();
        LinkedList<DiffUtil.Diff> diffs = diffList();
        diffUtil.diffCleanupEfficiency(diffs);
        assertEquals("diffCleanupEfficiency: Null case.", diffList(), diffs);

        diffs = diffList(new DiffUtil.Diff(DELETE, "ab"), new DiffUtil.Diff(INSERT, "12"), new DiffUtil.Diff(EQUAL, "wxyz"), new DiffUtil.Diff(DELETE, "cd"), new DiffUtil.Diff(INSERT, "34"));
        diffUtil.diffCleanupEfficiency(diffs);
        assertEquals("diffCleanupEfficiency: No elimination.", diffList(new DiffUtil.Diff(DELETE, "ab"), new DiffUtil.Diff(INSERT, "12"), new DiffUtil.Diff(EQUAL, "wxyz"), new DiffUtil.Diff(DELETE, "cd"), new DiffUtil.Diff(INSERT, "34")), diffs);

        diffs = diffList(new DiffUtil.Diff(DELETE, "ab"), new DiffUtil.Diff(INSERT, "12"), new DiffUtil.Diff(EQUAL, "xyz"), new DiffUtil.Diff(DELETE, "cd"), new DiffUtil.Diff(INSERT, "34"));
        diffUtil.diffCleanupEfficiency(diffs);
        assertEquals("diffCleanupEfficiency: Four-edit elimination.", diffList(new DiffUtil.Diff(DELETE, "abxyzcd"), new DiffUtil.Diff(INSERT, "12xyz34")), diffs);

        diffs = diffList(new DiffUtil.Diff(INSERT, "12"), new DiffUtil.Diff(EQUAL, "x"), new DiffUtil.Diff(DELETE, "cd"), new DiffUtil.Diff(INSERT, "34"));
        diffUtil.diffCleanupEfficiency(diffs);
        assertEquals("diffCleanupEfficiency: Three-edit elimination.", diffList(new DiffUtil.Diff(DELETE, "xcd"), new DiffUtil.Diff(INSERT, "12x34")), diffs);

        diffs = diffList(new DiffUtil.Diff(DELETE, "ab"), new DiffUtil.Diff(INSERT, "12"), new DiffUtil.Diff(EQUAL, "xy"), new DiffUtil.Diff(INSERT, "34"), new DiffUtil.Diff(EQUAL, "z"), new DiffUtil.Diff(DELETE, "cd"), new DiffUtil.Diff(INSERT, "56"));
        diffUtil.diffCleanupEfficiency(diffs);
        assertEquals("diffCleanupEfficiency: Backpass elimination.", diffList(new DiffUtil.Diff(DELETE, "abxyzcd"), new DiffUtil.Diff(INSERT, "12xy34z56")), diffs);

        diffUtil = DiffUtil.builder().diffEditCost(5).build();
        diffs = diffList(new DiffUtil.Diff(DELETE, "ab"), new DiffUtil.Diff(INSERT, "12"), new DiffUtil.Diff(EQUAL, "wxyz"), new DiffUtil.Diff(DELETE, "cd"), new DiffUtil.Diff(INSERT, "34"));
        diffUtil.diffCleanupEfficiency(diffs);
        assertEquals("diffCleanupEfficiency: High cost elimination.", diffList(new DiffUtil.Diff(DELETE, "abwxyzcd"), new DiffUtil.Diff(INSERT, "12wxyz34")), diffs);
    }

    @Test
    public void testDiffPrettyHtml() {
        // Pretty print.
        LinkedList<DiffUtil.Diff> diffs = diffList(new DiffUtil.Diff(EQUAL, "a\n"), new DiffUtil.Diff(DELETE, "<B>b</B>"), new DiffUtil.Diff(INSERT, "c&d"));
        assertEquals("diffPretyyHtml:", "<span>a&para;<br></span><del style=\"background:#ffe6e6;\">&lt;B&gt;b&lt;/B&gt;</del><ins style=\"background:#e6ffe6;\">c&amp;d</ins>", diffUtil.diffPretyyHtml(diffs));
    }

    @Test
    public void testDiffText() {
        // Compute the source and destination texts.
        LinkedList<DiffUtil.Diff> diffs = diffList(new DiffUtil.Diff(EQUAL, "jump"), new DiffUtil.Diff(DELETE, "s"), new DiffUtil.Diff(INSERT, "ed"), new DiffUtil.Diff(EQUAL, " over "), new DiffUtil.Diff(DELETE, "the"), new DiffUtil.Diff(INSERT, "a"), new DiffUtil.Diff(EQUAL, " lazy"));
        assertEquals("diffText1:", "jumps over the lazy", diffUtil.diffText1(diffs));
        assertEquals("diffText2:", "jumped over a lazy", diffUtil.diffText2(diffs));
    }

    @Test
    public void testDiffDelta() {
        // Convert a diff into delta string.
        LinkedList<DiffUtil.Diff> diffs = diffList(new DiffUtil.Diff(EQUAL, "jump"), new DiffUtil.Diff(DELETE, "s"), new DiffUtil.Diff(INSERT, "ed"), new DiffUtil.Diff(EQUAL, " over "), new DiffUtil.Diff(DELETE, "the"), new DiffUtil.Diff(INSERT, "a"), new DiffUtil.Diff(EQUAL, " lazy"), new DiffUtil.Diff(INSERT, "old dog"));
        String text1 = diffUtil.diffText1(diffs);
        assertEquals("diffText1: Base text.", "jumps over the lazy", text1);

        String delta = diffUtil.diffToDelta(diffs);
        assertEquals("diffToDelta:", "=4\t-1\t+ed\t=6\t-3\t+a\t=5\t+old dog", delta);

        // Convert delta string into a diff.
        assertEquals("diffFromDelta: Normal.", diffs, diffUtil.diffFromDelta(text1, delta));

        // Generates error (19 < 20).
        try {
            diffUtil.diffFromDelta(text1 + 'x', delta);
            fail("diffFromDelta: Too long.");
        } catch (IllegalArgumentException ex) {
            // Exception expected.
        }

        // Generates error (19 > 18).
        try {
            diffUtil.diffFromDelta(text1.substring(1), delta);
            fail("diffFromDelta: Too short.");
        } catch (IllegalArgumentException ex) {
            // Exception expected.
        }

        // Generates error (%c3%xy invalid Unicode).
        try {
            diffUtil.diffFromDelta("", "+%c3%xy");
            fail("diffFromDelta: Invalid character.");
        } catch (IllegalArgumentException ex) {
            // Exception expected.
        }

        // Test deltas with special characters.
        diffs = diffList(new DiffUtil.Diff(EQUAL, "\u0680 \000 \t %"), new DiffUtil.Diff(DELETE, "\u0681 \001 \n ^"), new DiffUtil.Diff(INSERT, "\u0682 \002 \\ |"));
        text1 = diffUtil.diffText1(diffs);
        assertEquals("diffText1: Unicode text.", "\u0680 \000 \t %\u0681 \001 \n ^", text1);

        delta = diffUtil.diffToDelta(diffs);
        assertEquals("diffToDelta: Unicode.", "=7\t-7\t+%DA%82 %02 %5C %7C", delta);

        assertEquals("diffFromDelta: Unicode.", diffs, diffUtil.diffFromDelta(text1, delta));

        // Verify pool of unchanged characters.
        diffs = diffList(new DiffUtil.Diff(INSERT, "A-Z a-z 0-9 - _ . ! ~ * ' ( ) ; / ? : @ & = + $ , # "));
        String text2 = diffUtil.diffText2(diffs);
        assertEquals("diffText2: Unchanged characters.", "A-Z a-z 0-9 - _ . ! ~ * \' ( ) ; / ? : @ & = + $ , # ", text2);

        delta = diffUtil.diffToDelta(diffs);
        assertEquals("diffToDelta: Unchanged characters.", "+A-Z a-z 0-9 - _ . ! ~ * \' ( ) ; / ? : @ & = + $ , # ", delta);

        // Convert delta string into a diff.
        assertEquals("diffFromDelta: Unchanged characters.", diffs, diffUtil.diffFromDelta("", delta));
    }

    @Test
    public void testDiffXIndex() {
        // Translate a location in text1 to text2.
        LinkedList<DiffUtil.Diff> diffs = diffList(new DiffUtil.Diff(DELETE, "a"), new DiffUtil.Diff(INSERT, "1234"), new DiffUtil.Diff(EQUAL, "xyz"));
        assertEquals("diffXIndex: Translation on equality.", 5, diffUtil.diffXIndex(diffs, 2));

        diffs = diffList(new DiffUtil.Diff(EQUAL, "a"), new DiffUtil.Diff(DELETE, "1234"), new DiffUtil.Diff(EQUAL, "xyz"));
        assertEquals("diffXIndex: Translation on deletion.", 1, diffUtil.diffXIndex(diffs, 3));
    }

    @Test
    public void testDiffLevenshtein() {
        LinkedList<DiffUtil.Diff> diffs = diffList(new DiffUtil.Diff(DELETE, "abc"), new DiffUtil.Diff(INSERT, "1234"), new DiffUtil.Diff(EQUAL, "xyz"));
        assertEquals("Levenshtein with trailing equality.", 4, diffUtil.diffLevenshtein(diffs));

        diffs = diffList(new DiffUtil.Diff(EQUAL, "xyz"), new DiffUtil.Diff(DELETE, "abc"), new DiffUtil.Diff(INSERT, "1234"));
        assertEquals("Levenshtein with leading equality.", 4, diffUtil.diffLevenshtein(diffs));

        diffs = diffList(new DiffUtil.Diff(DELETE, "abc"), new DiffUtil.Diff(EQUAL, "xyz"), new DiffUtil.Diff(INSERT, "1234"));
        assertEquals("Levenshtein with middle equality.", 7, diffUtil.diffLevenshtein(diffs));
    }

    @Test
    public void testDiffBisect() {
        // Normal.
        String a = "cat";
        String b = "map";
        // Since the resulting diff hasn't been normalized, it would be ok if
        // the insertion and deletion pairs are swapped.
        // If the order changes, tweak this test as required.
        LinkedList<DiffUtil.Diff> diffs = diffList(new DiffUtil.Diff(DELETE, "c"), new DiffUtil.Diff(INSERT, "m"), new DiffUtil.Diff(EQUAL, "a"), new DiffUtil.Diff(DELETE, "t"), new DiffUtil.Diff(INSERT, "p"));
        assertEquals("diffBisect: Normal.", diffs, diffUtil.diffBisect(a, b, Long.MAX_VALUE));

        // Timeout.
        diffs = diffList(new DiffUtil.Diff(DELETE, "cat"), new DiffUtil.Diff(INSERT, "map"));
        assertEquals("diffBisect: Timeout.", diffs, diffUtil.diffBisect(a, b, 0));
    }

    @Test
    public void testDiffMain() {
        // Perform a trivial diff.
        LinkedList<DiffUtil.Diff> diffs = diffList();
        assertEquals("diffMain: Null case.", diffs, diffUtil.diffMain("", "", false));

        diffs = diffList(new DiffUtil.Diff(EQUAL, "abc"));
        assertEquals("diffMain: Equality.", diffs, diffUtil.diffMain("abc", "abc", false));

        diffs = diffList(new DiffUtil.Diff(EQUAL, "ab"), new DiffUtil.Diff(INSERT, "123"), new DiffUtil.Diff(EQUAL, "c"));
        assertEquals("diffMain: Simple insertion.", diffs, diffUtil.diffMain("abc", "ab123c", false));

        diffs = diffList(new DiffUtil.Diff(EQUAL, "a"), new DiffUtil.Diff(DELETE, "123"), new DiffUtil.Diff(EQUAL, "bc"));
        assertEquals("diffMain: Simple deletion.", diffs, diffUtil.diffMain("a123bc", "abc", false));

        diffs = diffList(new DiffUtil.Diff(EQUAL, "a"), new DiffUtil.Diff(INSERT, "123"), new DiffUtil.Diff(EQUAL, "b"), new DiffUtil.Diff(INSERT, "456"), new DiffUtil.Diff(EQUAL, "c"));
        assertEquals("diffMain: Two insertions.", diffs, diffUtil.diffMain("abc", "a123b456c", false));

        diffs = diffList(new DiffUtil.Diff(EQUAL, "a"), new DiffUtil.Diff(DELETE, "123"), new DiffUtil.Diff(EQUAL, "b"), new DiffUtil.Diff(DELETE, "456"), new DiffUtil.Diff(EQUAL, "c"));
        assertEquals("diffMain: Two deletions.", diffs, diffUtil.diffMain("a123b456c", "abc", false));

        // Perform a real diff.
        // Switch off the patchTimeout.
        diffUtil = DiffUtil.builder().patchTimeout(0).build();
        diffs = diffList(new DiffUtil.Diff(DELETE, "a"), new DiffUtil.Diff(INSERT, "b"));
        assertEquals("diffMain: Simple case #1.", diffs, diffUtil.diffMain("a", "b", false));

        diffs = diffList(new DiffUtil.Diff(DELETE, "Apple"), new DiffUtil.Diff(INSERT, "Banana"), new DiffUtil.Diff(EQUAL, "s are a"), new DiffUtil.Diff(INSERT, "lso"), new DiffUtil.Diff(EQUAL, " fruit."));
        assertEquals("diffMain: Simple case #2.", diffs, diffUtil.diffMain("Apples are a fruit.", "Bananas are also fruit.", false));

        diffs = diffList(new DiffUtil.Diff(DELETE, "a"), new DiffUtil.Diff(INSERT, "\u0680"), new DiffUtil.Diff(EQUAL, "x"), new DiffUtil.Diff(DELETE, "\t"), new DiffUtil.Diff(INSERT, "\000"));
        assertEquals("diffMain: Simple case #3.", diffs, diffUtil.diffMain("ax\t", "\u0680x\000", false));

        diffs = diffList(new DiffUtil.Diff(DELETE, "1"), new DiffUtil.Diff(EQUAL, "a"), new DiffUtil.Diff(DELETE, "y"), new DiffUtil.Diff(EQUAL, "b"), new DiffUtil.Diff(DELETE, "2"), new DiffUtil.Diff(INSERT, "xab"));
        assertEquals("diffMain: Overlap #1.", diffs, diffUtil.diffMain("1ayb2", "abxab", false));

        diffs = diffList(new DiffUtil.Diff(INSERT, "xaxcx"), new DiffUtil.Diff(EQUAL, "abc"), new DiffUtil.Diff(DELETE, "y"));
        assertEquals("diffMain: Overlap #2.", diffs, diffUtil.diffMain("abcy", "xaxcxabc", false));

        diffs = diffList(new DiffUtil.Diff(DELETE, "ABCD"), new DiffUtil.Diff(EQUAL, "a"), new DiffUtil.Diff(DELETE, "="), new DiffUtil.Diff(INSERT, "-"), new DiffUtil.Diff(EQUAL, "bcd"), new DiffUtil.Diff(DELETE, "="), new DiffUtil.Diff(INSERT, "-"), new DiffUtil.Diff(EQUAL, "efghijklmnopqrs"), new DiffUtil.Diff(DELETE, "EFGHIJKLMNOefg"));
        assertEquals("diffMain: Overlap #3.", diffs, diffUtil.diffMain("ABCDa=bcd=efghijklmnopqrsEFGHIJKLMNOefg", "a-bcd-efghijklmnopqrs", false));

        diffs = diffList(new DiffUtil.Diff(INSERT, " "), new DiffUtil.Diff(EQUAL, "a"), new DiffUtil.Diff(INSERT, "nd"), new DiffUtil.Diff(EQUAL, " [[Pennsylvania]]"), new DiffUtil.Diff(DELETE, " and [[New"));
        assertEquals("diffMain: Large equality.", diffs, diffUtil.diffMain("a [[Pennsylvania]] and [[New", " and [[Pennsylvania]]", false));

        diffUtil = DiffUtil.builder().patchTimeout(0.1f).build();
        String a = "`Twas brillig, and the slithy toves\nDid gyre and gimble in the wabe:\nAll mimsy were the borogoves,\nAnd the mome raths outgrabe.\n";
        String b = "I am the very model of a modern major general,\nI've information vegetable, animal, and mineral,\nI know the kings of England, and I quote the fights historical,\nFrom Marathon to Waterloo, in order categorical.\n";
        // Increase the text lengths by 1024 times to ensure a patchTimeout.
        for (int x = 0; x < 10; x++) {
            a = a + a;
            b = b + b;
        }
        long startTime = System.currentTimeMillis();
        diffUtil.diffMain(a, b);
        long endTime = System.currentTimeMillis();
        // Test that we took at least the patchTimeout period.
        assertTrue("diffMain: Timeout min.", diffUtil.diffTimeout() * 1000 <= endTime - startTime);
        // Test that we didn't take forever (be forgiving).
        // Theoretically this test could fail very occasionally if the
        // OS task swaps or locks up for a second at the wrong moment.
        assertTrue("diffMain: Timeout max.", diffUtil.diffTimeout() * 1000 * 2 > endTime - startTime);
        diffUtil = DiffUtil.builder().patchTimeout(0).build();

        // Test the linemode speedup.
        // Must be long to pass the 100 char cutoff.
        a = "1234567890\n1234567890\n1234567890\n1234567890\n1234567890\n1234567890\n1234567890\n1234567890\n1234567890\n1234567890\n1234567890\n1234567890\n1234567890\n";
        b = "abcdefghij\nabcdefghij\nabcdefghij\nabcdefghij\nabcdefghij\nabcdefghij\nabcdefghij\nabcdefghij\nabcdefghij\nabcdefghij\nabcdefghij\nabcdefghij\nabcdefghij\n";
        assertEquals("diffMain: Simple line-mode.", diffUtil.diffMain(a, b, true), diffUtil.diffMain(a, b, false));

        a = "1234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890";
        b = "abcdefghijabcdefghijabcdefghijabcdefghijabcdefghijabcdefghijabcdefghijabcdefghijabcdefghijabcdefghijabcdefghijabcdefghijabcdefghij";
        assertEquals("diffMain: Single line-mode.", diffUtil.diffMain(a, b, true), diffUtil.diffMain(a, b, false));

        a = "1234567890\n1234567890\n1234567890\n1234567890\n1234567890\n1234567890\n1234567890\n1234567890\n1234567890\n1234567890\n1234567890\n1234567890\n1234567890\n";
        b = "abcdefghij\n1234567890\n1234567890\n1234567890\nabcdefghij\n1234567890\n1234567890\n1234567890\nabcdefghij\n1234567890\n1234567890\n1234567890\nabcdefghij\n";
        String[] texts_linemode = diff_rebuildtexts(diffUtil.diffMain(a, b, true));
        String[] texts_textmode = diff_rebuildtexts(diffUtil.diffMain(a, b, false));
        assertArrayEquals("diffMain: Overlap line-mode.", texts_textmode, texts_linemode);

        // Test null inputs.
        try {
            diffUtil.diffMain(null, null);
            fail("diffMain: Null inputs.");
        } catch (IllegalArgumentException ex) {
            // Error expected.
        }
    }


    //  MATCH TEST FUNCTIONS


    @Test
    public void testMatchAlphabet() {
        // Initialise the bitmasks for Bitap.
        Map<Character, Integer> bitmask;
        bitmask = new HashMap<Character, Integer>();
        bitmask.put('a', 4); bitmask.put('b', 2); bitmask.put('c', 1);
        assertEquals("matchAlpabet: Unique.", bitmask, diffUtil.matchAlpabet("abc"));

        bitmask = new HashMap<Character, Integer>();
        bitmask.put('a', 37); bitmask.put('b', 18); bitmask.put('c', 8);
        assertEquals("matchAlpabet: Duplicates.", bitmask, diffUtil.matchAlpabet("abcaba"));
    }

    @Test
    public void testMatchBitap() {
        // Bitap algorithm.
        diffUtil = DiffUtil.builder().matchDistance(100).matchThreshold(0.5f).build();
        assertEquals("matchBitmap: Exact match #1.", 5, diffUtil.matchBitmap("abcdefghijk", "fgh", 5));

        assertEquals("matchBitmap: Exact match #2.", 5, diffUtil.matchBitmap("abcdefghijk", "fgh", 0));

        assertEquals("matchBitmap: Fuzzy match #1.", 4, diffUtil.matchBitmap("abcdefghijk", "efxhi", 0));

        assertEquals("matchBitmap: Fuzzy match #2.", 2, diffUtil.matchBitmap("abcdefghijk", "cdefxyhijk", 5));

        assertEquals("matchBitmap: Fuzzy match #3.", -1, diffUtil.matchBitmap("abcdefghijk", "bxy", 1));

        assertEquals("matchBitmap: Overflow.", 2, diffUtil.matchBitmap("123456789xx0", "3456789x0", 2));

        assertEquals("matchBitmap: Before start match.", 0, diffUtil.matchBitmap("abcdef", "xxabc", 4));

        assertEquals("matchBitmap: Beyond end match.", 3, diffUtil.matchBitmap("abcdef", "defyy", 4));

        assertEquals("matchBitmap: Oversized pattern.", 0, diffUtil.matchBitmap("abcdef", "xabcdefy", 0));

        diffUtil = DiffUtil.builder().matchDistance(100).matchThreshold(0.4f).build();
        assertEquals("matchBitmap: Threshold #1.", 4, diffUtil.matchBitmap("abcdefghijk", "efxyhi", 1));

        diffUtil = DiffUtil.builder().matchDistance(100).matchThreshold(0.3f).build();
        assertEquals("matchBitmap: Threshold #2.", -1, diffUtil.matchBitmap("abcdefghijk", "efxyhi", 1));

        diffUtil = DiffUtil.builder().matchDistance(100).matchThreshold(0.0f).build();
        assertEquals("matchBitmap: Threshold #3.", 1, diffUtil.matchBitmap("abcdefghijk", "bcdef", 1));

        diffUtil = DiffUtil.builder().matchDistance(100).matchThreshold(0.5f).build();
        assertEquals("matchBitmap: Multiple select #1.", 0, diffUtil.matchBitmap("abcdexyzabcde", "abccde", 3));

        assertEquals("matchBitmap: Multiple select #2.", 8, diffUtil.matchBitmap("abcdexyzabcde", "abccde", 5));

        diffUtil = DiffUtil.builder().matchDistance(10).matchThreshold(0.5f).build();
        assertEquals("matchBitmap: Distance test #1.", -1, diffUtil.matchBitmap("abcdefghijklmnopqrstuvwxyz", "abcdefg", 24));

        assertEquals("matchBitmap: Distance test #2.", 0, diffUtil.matchBitmap("abcdefghijklmnopqrstuvwxyz", "abcdxxefg", 1));

        diffUtil = DiffUtil.builder().matchDistance(1000).matchThreshold(0.5f).build();
        assertEquals("matchBitmap: Distance test #3.", 0, diffUtil.matchBitmap("abcdefghijklmnopqrstuvwxyz", "abcdefg", 24));
    }

    @Test
    public void testMatchMain() {
        // Full match.
        assertEquals("matchMain: Equality.", 0, diffUtil.matchMain("abcdef", "abcdef", 1000));

        assertEquals("matchMain: Null text.", -1, diffUtil.matchMain("", "abcdef", 1));

        assertEquals("matchMain: Null pattern.", 3, diffUtil.matchMain("abcdef", "", 3));

        assertEquals("matchMain: Exact match.", 3, diffUtil.matchMain("abcdef", "de", 3));

        assertEquals("matchMain: Beyond end match.", 3, diffUtil.matchMain("abcdef", "defy", 4));

        assertEquals("matchMain: Oversized pattern.", 0, diffUtil.matchMain("abcdef", "abcdefy", 0));

        diffUtil = DiffUtil.builder().matchThreshold(0.7f).build();
        //diffUtil.Match_Threshold = 0.7f;
        assertEquals("matchMain: Complex match.", 4, diffUtil.matchMain("I am the very model of a modern major general.", " that berry ", 5));
        diffUtil = DiffUtil.builder().matchThreshold(0.5f).build();
        //diffUtil.Match_Threshold = 0.5f;

        // Test null inputs.
        try {
            diffUtil.matchMain(null, null, 0);
            fail("matchMain: Null inputs.");
        } catch (IllegalArgumentException ex) {
            // Error expected.
        }
    }


    //  PATCH TEST FUNCTIONS


    @Test
    public void testPatchObj() {
        // Patch Object.
        DiffUtil.Patch p = new DiffUtil.Patch();
        p.start1 = 20;
        p.start2 = 21;
        p.length1 = 18;
        p.length2 = 17;
        p.diffs = diffList(new DiffUtil.Diff(EQUAL, "jump"), new DiffUtil.Diff(DELETE, "s"), new DiffUtil.Diff(INSERT, "ed"), new DiffUtil.Diff(EQUAL, " over "), new DiffUtil.Diff(DELETE, "the"), new DiffUtil.Diff(INSERT, "a"), new DiffUtil.Diff(EQUAL, "\nlaz"));
        String strp = "@@ -21,18 +22,17 @@\n jump\n-s\n+ed\n  over \n-the\n+a\n %0Alaz\n";
        assertEquals("Patch: toString.", strp, p.toString());
    }

    @Test
    public void testPatchFromText() {
        assertTrue("patchFromText: #0.", diffUtil.patchFromText("").isEmpty());

        String strp = "@@ -21,18 +22,17 @@\n jump\n-s\n+ed\n  over \n-the\n+a\n %0Alaz\n";
        assertEquals("patchFromText: #1.", strp, diffUtil.patchFromText(strp).get(0).toString());

        assertEquals("patchFromText: #2.", "@@ -1 +1 @@\n-a\n+b\n", diffUtil.patchFromText("@@ -1 +1 @@\n-a\n+b\n").get(0).toString());

        assertEquals("patchFromText: #3.", "@@ -1,3 +0,0 @@\n-abc\n", diffUtil.patchFromText("@@ -1,3 +0,0 @@\n-abc\n").get(0).toString());

        assertEquals("patchFromText: #4.", "@@ -0,0 +1,3 @@\n+abc\n", diffUtil.patchFromText("@@ -0,0 +1,3 @@\n+abc\n").get(0).toString());

        // Generates error.
        try {
            diffUtil.patchFromText("Bad\nPatch\n");
            fail("patchFromText: #5.");
        } catch (IllegalArgumentException ex) {
            // Exception expected.
        }
    }

    @Test
    public void testPatchToText() {
        String strp = "@@ -21,18 +22,17 @@\n jump\n-s\n+ed\n  over \n-the\n+a\n  laz\n";
        List<DiffUtil.Patch> patches;
        patches = diffUtil.patchFromText(strp);
        assertEquals("patchToText: Single.", strp, diffUtil.patchToText(patches));

        strp = "@@ -1,9 +1,9 @@\n-f\n+F\n oo+fooba\n@@ -7,9 +7,9 @@\n obar\n-,\n+.\n  tes\n";
        patches = diffUtil.patchFromText(strp);
        assertEquals("patchToText: Dual.", strp, diffUtil.patchToText(patches));
    }

    @Test
    public void testPatchAddContext() {
        diffUtil = DiffUtil.builder().patchMargin(4).build();
        //diffUtil.Patch_Margin = 4;
        DiffUtil.Patch p;
        p = diffUtil.patchFromText("@@ -21,4 +21,10 @@\n-jump\n+somersault\n").get(0);
        diffUtil.patchAddContext(p, "The quick brown fox jumps over the lazy dog.");
        assertEquals("patchAddContext: Simple case.", "@@ -17,12 +17,18 @@\n fox \n-jump\n+somersault\n s ov\n", p.toString());

        p = diffUtil.patchFromText("@@ -21,4 +21,10 @@\n-jump\n+somersault\n").get(0);
        diffUtil.patchAddContext(p, "The quick brown fox jumps.");
        assertEquals("patchAddContext: Not enough trailing context.", "@@ -17,10 +17,16 @@\n fox \n-jump\n+somersault\n s.\n", p.toString());

        p = diffUtil.patchFromText("@@ -3 +3,2 @@\n-e\n+at\n").get(0);
        diffUtil.patchAddContext(p, "The quick brown fox jumps.");
        assertEquals("patchAddContext: Not enough leading context.", "@@ -1,7 +1,8 @@\n Th\n-e\n+at\n  qui\n", p.toString());

        p = diffUtil.patchFromText("@@ -3 +3,2 @@\n-e\n+at\n").get(0);
        diffUtil.patchAddContext(p, "The quick brown fox jumps.  The quick brown fox crashes.");
        assertEquals("patchAddContext: Ambiguity.", "@@ -1,27 +1,28 @@\n Th\n-e\n+at\n  quick brown fox jumps. \n", p.toString());
    }

    @SuppressWarnings("deprecation")
    @Test
    public void testPatchMake() {
        LinkedList<DiffUtil.Patch> patches;
        patches = diffUtil.patchMake("", "");
        assertEquals("patchMake: Null case.", "", diffUtil.patchToText(patches));

        String text1 = "The quick brown fox jumps over the lazy dog.";
        String text2 = "That quick brown fox jumped over a lazy dog.";
        String expectedPatch = "@@ -1,8 +1,7 @@\n Th\n-at\n+e\n  qui\n@@ -21,17 +21,18 @@\n jump\n-ed\n+s\n  over \n-a\n+the\n  laz\n";
        // The second patch must be "-21,17 +21,18", not "-22,17 +21,18" due to rolling context.
        patches = diffUtil.patchMake(text2, text1);
        assertEquals("patchMake: Text2+Text1 inputs.", expectedPatch, diffUtil.patchToText(patches));

        expectedPatch = "@@ -1,11 +1,12 @@\n Th\n-e\n+at\n  quick b\n@@ -22,18 +22,17 @@\n jump\n-s\n+ed\n  over \n-the\n+a\n  laz\n";
        patches = diffUtil.patchMake(text1, text2);
        assertEquals("patchMake: Text1+Text2 inputs.", expectedPatch, diffUtil.patchToText(patches));

        LinkedList<DiffUtil.Diff> diffs = diffUtil.diffMain(text1, text2, false);
        patches = diffUtil.patchMake(diffs);
        assertEquals("patchMake: Diff input.", expectedPatch, diffUtil.patchToText(patches));

        patches = diffUtil.patchMake(text1, diffs);
        assertEquals("patchMake: Text1+Diff inputs.", expectedPatch, diffUtil.patchToText(patches));

        patches = diffUtil.patchMake(text1, text2, diffs);
        assertEquals("patchMake: Text1+Text2+Diff inputs (deprecated).", expectedPatch, diffUtil.patchToText(patches));

        patches = diffUtil.patchMake("`1234567890-=[]\\;',./", "~!@#$%^&*()_+{}|:\"<>?");
        assertEquals("patchToText: Character encoding.", "@@ -1,21 +1,21 @@\n-%601234567890-=%5B%5D%5C;',./\n+~!@#$%25%5E&*()_+%7B%7D%7C:%22%3C%3E?\n", diffUtil.patchToText(patches));

        diffs = diffList(new DiffUtil.Diff(DELETE, "`1234567890-=[]\\;',./"), new DiffUtil.Diff(INSERT, "~!@#$%^&*()_+{}|:\"<>?"));
        assertEquals("patchFromText: Character decoding.", diffs, diffUtil.patchFromText("@@ -1,21 +1,21 @@\n-%601234567890-=%5B%5D%5C;',./\n+~!@#$%25%5E&*()_+%7B%7D%7C:%22%3C%3E?\n").get(0).diffs);

        text1 = "";
        for (int x = 0; x < 100; x++) {
            text1 += "abcdef";
        }
        text2 = text1 + "123";
        expectedPatch = "@@ -573,28 +573,31 @@\n cdefabcdefabcdefabcdefabcdef\n+123\n";
        patches = diffUtil.patchMake(text1, text2);
        assertEquals("patchMake: Long string with repeats.", expectedPatch, diffUtil.patchToText(patches));

        // Test null inputs.
        try {
            diffUtil.patchMake(null);
            fail("patchMake: Null inputs.");
        } catch (IllegalArgumentException ex) {
            // Error expected.
        }
    }

    @Test
    public void testPatchSplitMax() {
        // Assumes that Match_MaxBits is 32.
        LinkedList<DiffUtil.Patch> patches;
        patches = diffUtil.patchMake("abcdefghijklmnopqrstuvwxyz01234567890", "XabXcdXefXghXijXklXmnXopXqrXstXuvXwxXyzX01X23X45X67X89X0");
        diffUtil.patchSplitMax(patches);
        assertEquals("patchSplitMax: #1.", "@@ -1,32 +1,46 @@\n+X\n ab\n+X\n cd\n+X\n ef\n+X\n gh\n+X\n ij\n+X\n kl\n+X\n mn\n+X\n op\n+X\n qr\n+X\n st\n+X\n uv\n+X\n wx\n+X\n yz\n+X\n 012345\n@@ -25,13 +39,18 @@\n zX01\n+X\n 23\n+X\n 45\n+X\n 67\n+X\n 89\n+X\n 0\n", diffUtil.patchToText(patches));

        patches = diffUtil.patchMake("abcdef1234567890123456789012345678901234567890123456789012345678901234567890uvwxyz", "abcdefuvwxyz");
        String oldToText = diffUtil.patchToText(patches);
        diffUtil.patchSplitMax(patches);
        assertEquals("patchSplitMax: #2.", oldToText, diffUtil.patchToText(patches));

        patches = diffUtil.patchMake("1234567890123456789012345678901234567890123456789012345678901234567890", "abc");
        diffUtil.patchSplitMax(patches);
        assertEquals("patchSplitMax: #3.", "@@ -1,32 +1,4 @@\n-1234567890123456789012345678\n 9012\n@@ -29,32 +1,4 @@\n-9012345678901234567890123456\n 7890\n@@ -57,14 +1,3 @@\n-78901234567890\n+abc\n", diffUtil.patchToText(patches));

        patches = diffUtil.patchMake("abcdefghij , h : 0 , t : 1 abcdefghij , h : 0 , t : 1 abcdefghij , h : 0 , t : 1", "abcdefghij , h : 1 , t : 1 abcdefghij , h : 1 , t : 1 abcdefghij , h : 0 , t : 1");
        diffUtil.patchSplitMax(patches);
        assertEquals("patchSplitMax: #4.", "@@ -2,32 +2,32 @@\n bcdefghij , h : \n-0\n+1\n  , t : 1 abcdef\n@@ -29,32 +29,32 @@\n bcdefghij , h : \n-0\n+1\n  , t : 1 abcdef\n", diffUtil.patchToText(patches));
    }

    @Test
    public void testPatchAddPadding() {
        LinkedList<DiffUtil.Patch> patches;
        patches = diffUtil.patchMake("", "test");
        assertEquals("patchAddPadding: Both edges full.", "@@ -0,0 +1,4 @@\n+test\n", diffUtil.patchToText(patches));
        diffUtil.patchAddPadding(patches);
        assertEquals("patchAddPadding: Both edges full.", "@@ -1,8 +1,12 @@\n %01%02%03%04\n+test\n %01%02%03%04\n", diffUtil.patchToText(patches));

        patches = diffUtil.patchMake("XY", "XtestY");
        assertEquals("patchAddPadding: Both edges partial.", "@@ -1,2 +1,6 @@\n X\n+test\n Y\n", diffUtil.patchToText(patches));
        diffUtil.patchAddPadding(patches);
        assertEquals("patchAddPadding: Both edges partial.", "@@ -2,8 +2,12 @@\n %02%03%04X\n+test\n Y%01%02%03\n", diffUtil.patchToText(patches));

        patches = diffUtil.patchMake("XXXXYYYY", "XXXXtestYYYY");
        assertEquals("patchAddPadding: Both edges none.", "@@ -1,8 +1,12 @@\n XXXX\n+test\n YYYY\n", diffUtil.patchToText(patches));
        diffUtil.patchAddPadding(patches);
        assertEquals("patchAddPadding: Both edges none.", "@@ -5,8 +5,12 @@\n XXXX\n+test\n YYYY\n", diffUtil.patchToText(patches));
    }

    @Test
    public void testPatchApply() {
        diffUtil = DiffUtil.builder().matchDistance(1000).matchThreshold(0.5f).patchDeleteThreshold(0.5f).build();
        //diffUtil.Match_Distance = 1000;
        //diffUtil.Match_Threshold = 0.5f;
        //diffUtil.Patch_DeleteThreshold = 0.5f;
        LinkedList<DiffUtil.Patch> patches;
        patches = diffUtil.patchMake("", "");
        Object[] results = diffUtil.patchApply(patches, "Hello world.");
        boolean[] boolArray = (boolean[]) results[1];
        String resultStr = results[0] + "\t" + boolArray.length;
        assertEquals("patchApply: Null case.", "Hello world.\t0", resultStr);

        patches = diffUtil.patchMake("The quick brown fox jumps over the lazy dog.", "That quick brown fox jumped over a lazy dog.");
        results = diffUtil.patchApply(patches, "The quick brown fox jumps over the lazy dog.");
        boolArray = (boolean[]) results[1];
        resultStr = results[0] + "\t" + boolArray[0] + '\t' + boolArray[1];
        assertEquals("patchApply: Exact match.", "That quick brown fox jumped over a lazy dog.\ttrue\ttrue", resultStr);

        results = diffUtil.patchApply(patches, "The quick red rabbit jumps over the tired tiger.");
        boolArray = (boolean[]) results[1];
        resultStr = results[0] + "\t" + boolArray[0] + '\t' + boolArray[1];
        assertEquals("patchApply: Partial match.", "That quick red rabbit jumped over a tired tiger.\ttrue\ttrue", resultStr);

        results = diffUtil.patchApply(patches, "I am the very model of a modern major general.");
        boolArray = (boolean[]) results[1];
        resultStr = results[0] + "\t" + boolArray[0] + '\t' + boolArray[1];
        assertEquals("patchApply: Failed match.", "I am the very model of a modern major general.\tfalse\tfalse", resultStr);

        patches = diffUtil.patchMake("x1234567890123456789012345678901234567890123456789012345678901234567890y", "xabcy");
        results = diffUtil.patchApply(patches, "x123456789012345678901234567890-----++++++++++-----123456789012345678901234567890y");
        boolArray = (boolean[]) results[1];
        resultStr = results[0] + "\t" + boolArray[0] + '\t' + boolArray[1];
        assertEquals("patchApply: Big delete, small change.", "xabcy\ttrue\ttrue", resultStr);

        patches = diffUtil.patchMake("x1234567890123456789012345678901234567890123456789012345678901234567890y", "xabcy");
        results = diffUtil.patchApply(patches, "x12345678901234567890---------------++++++++++---------------12345678901234567890y");
        boolArray = (boolean[]) results[1];
        resultStr = results[0] + "\t" + boolArray[0] + '\t' + boolArray[1];
        assertEquals("patchApply: Big delete, big change 1.", "xabc12345678901234567890---------------++++++++++---------------12345678901234567890y\tfalse\ttrue", resultStr);

        diffUtil = DiffUtil.builder().matchDistance(1000).matchThreshold(0.5f).patchDeleteThreshold(0.6f).build();
        //diffUtil.Patch_DeleteThreshold = 0.6f;
        patches = diffUtil.patchMake("x1234567890123456789012345678901234567890123456789012345678901234567890y", "xabcy");
        results = diffUtil.patchApply(patches, "x12345678901234567890---------------++++++++++---------------12345678901234567890y");
        boolArray = (boolean[]) results[1];
        resultStr = results[0] + "\t" + boolArray[0] + '\t' + boolArray[1];
        assertEquals("patchApply: Big delete, big change 2.", "xabcy\ttrue\ttrue", resultStr);

        diffUtil = DiffUtil.builder().matchDistance(0).matchThreshold(0.0f).patchDeleteThreshold(0.5f).build();
        //diffUtil.Patch_DeleteThreshold = 0.5f;
        // Compensate for failed patch.
        //diffUtil.Match_Threshold = 0.0f;
        //diffUtil.Match_Distance = 0;
        patches = diffUtil.patchMake("abcdefghijklmnopqrstuvwxyz--------------------1234567890", "abcXXXXXXXXXXdefghijklmnopqrstuvwxyz--------------------1234567YYYYYYYYYY890");
        results = diffUtil.patchApply(patches, "ABCDEFGHIJKLMNOPQRSTUVWXYZ--------------------1234567890");
        boolArray = (boolean[]) results[1];
        resultStr = results[0] + "\t" + boolArray[0] + '\t' + boolArray[1];
        assertEquals("patchApply: Compensate for failed patch.", "ABCDEFGHIJKLMNOPQRSTUVWXYZ--------------------1234567YYYYYYYYYY890\tfalse\ttrue", resultStr);
        diffUtil = DiffUtil.builder().matchDistance(1000).matchThreshold(0.5f).patchDeleteThreshold(0.5f).build();
        //diffUtil.Match_Threshold = 0.5f;
        //diffUtil.Match_Distance = 1000;

        patches = diffUtil.patchMake("", "test");
        String patchStr = diffUtil.patchToText(patches);
        diffUtil.patchApply(patches, "");
        assertEquals("patchApply: No side effects.", patchStr, diffUtil.patchToText(patches));

        patches = diffUtil.patchMake("The quick brown fox jumps over the lazy dog.", "Woof");
        patchStr = diffUtil.patchToText(patches);
        diffUtil.patchApply(patches, "The quick brown fox jumps over the lazy dog.");
        assertEquals("patchApply: No side effects with major delete.", patchStr, diffUtil.patchToText(patches));

        patches = diffUtil.patchMake("", "test");
        results = diffUtil.patchApply(patches, "");
        boolArray = (boolean[]) results[1];
        resultStr = results[0] + "\t" + boolArray[0];
        assertEquals("patchApply: Edge exact match.", "test\ttrue", resultStr);

        patches = diffUtil.patchMake("XY", "XtestY");
        results = diffUtil.patchApply(patches, "XY");
        boolArray = (boolean[]) results[1];
        resultStr = results[0] + "\t" + boolArray[0];
        assertEquals("patchApply: Near edge exact match.", "XtestY\ttrue", resultStr);

        patches = diffUtil.patchMake("y", "y123");
        results = diffUtil.patchApply(patches, "x");
        boolArray = (boolean[]) results[1];
        resultStr = results[0] + "\t" + boolArray[0];
        assertEquals("patchApply: Edge partial match.", "x123\ttrue", resultStr);
    }

    private static void assertArrayEquals(String error_msg, Object[] a, Object[] b) {
        List<Object> list_a = Arrays.asList(a);
        List<Object> list_b = Arrays.asList(b);
        assertEquals(error_msg, list_a, list_b);
    }

    private static void assertLinesToCharsResultEquals(String error_msg,
                                                DiffUtil.LinesToCharsResult a, DiffUtil.LinesToCharsResult b) {
        assertEquals(error_msg, a.chars1, b.chars1);
        assertEquals(error_msg, a.chars2, b.chars2);
        assertEquals(error_msg, a.lineArray, b.lineArray);
    }

    // Construct the two texts which made up the diff originally.
    private static String[] diff_rebuildtexts(LinkedList<DiffUtil.Diff> diffs) {
        String[] text = {"", ""};
        for (DiffUtil.Diff myDiff : diffs) {
            if (myDiff.operation != DiffUtil.Operation.INSERT) {
                text[0] += myDiff.text;
            }
            if (myDiff.operation != DiffUtil.Operation.DELETE) {
                text[1] += myDiff.text;
            }
        }
        return text;
    }

    private static LinkedList<DiffUtil.Diff> diffList(DiffUtil.Diff... diffs) {
        return new LinkedList<DiffUtil.Diff>(Arrays.asList(diffs));
    }
}

