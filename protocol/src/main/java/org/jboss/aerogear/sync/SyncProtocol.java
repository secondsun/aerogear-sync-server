package org.jboss.aerogear.sync;

public interface SyncProtocol {

    int id();

    int revision();

    String data();

}
