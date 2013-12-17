package org.jboss.aerogear.sync;

import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.*;

public class SyncManagerTest {

    @Test
    public void messageRecieved() {
        final EmbeddedChannel channel = new EmbeddedChannel(new SyncManager());
        channel.writeInbound("some data");
        final Object o = channel.readOutbound();
        assertThat(o, is(notNullValue()));
        assertThat(o, is(instanceOf(String.class)));
    }
}
