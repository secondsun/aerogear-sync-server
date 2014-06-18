package org.jboss.aerogear.sync.diffsync;

public class DefaultBackupShadowDocument<T> implements BackupShadowDocument<T> {

    private final long version;
    private final ShadowDocument<T> shadow;

    public DefaultBackupShadowDocument(final long version, final ShadowDocument<T> shadow) {
        this.version = version;
        this.shadow = shadow;
    }

    @Override
    public long version() {
        return version;
    }

    @Override
    public ShadowDocument<T> shadow() {
        return shadow;
    }
}
