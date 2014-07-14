package org.jboss.aerogear.diffsync.client;

import org.jboss.aerogear.diffsync.DefaultDiff;
import org.jboss.aerogear.diffsync.DefaultEdit;
import org.jboss.aerogear.diffsync.Diff;
import org.jboss.aerogear.diffsync.Diff.Operation;
import org.jboss.aerogear.diffsync.Edit;

import java.util.LinkedList;

public class EditBuilder {

    private final String documentId;
    private String clientId;
    private long serverVersion;
    private long clientVersion;
    private String checksum;
    private final LinkedList<Diff> diffs = new LinkedList<Diff>();

    public EditBuilder(final String documentId) {
        this.documentId = documentId;
    }

    public EditBuilder clientId(final String clientId) {
        this.clientId = clientId;
        return this;
    }

    public EditBuilder serverVersion(final long serverVersion) {
        this.serverVersion = serverVersion;
        return this;
    }

    public EditBuilder clientVersion(final long clientVersion) {
        this.clientVersion = clientVersion;
        return this;
    }

    public EditBuilder unchanged(final String text) {
        diffs.add(new DefaultDiff(Operation.UNCHANGED, text));
        return this;
    }

    public EditBuilder add(final String text) {
        diffs.add(new DefaultDiff(Operation.ADD, text));
        return this;
    }

    public EditBuilder delete(final String text) {
        diffs.add(new DefaultDiff(Operation.DELETE, text));
        return this;
    }

    public EditBuilder checksum(final String checksum) {
        this.checksum = checksum;
        return this;
    }

    public Edit build() {
        return new DefaultEdit(documentId, clientId, clientVersion, serverVersion, checksum, diffs);
    }

}
