/*
 * Copyright 2014 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package org.jboss.aerogear.sync.alpn;

import io.netty.handler.codec.spdy.SpdyOrHttpChooser;
import org.eclipse.jetty.alpn.ALPN.ServerProvider;

import java.util.List;
import javax.net.ssl.SSLEngine;
import org.eclipse.jetty.alpn.ALPN;

/**
 * This class was lifted from Netty's spdy example.
 */
public class SpdyServerProvider implements ServerProvider {

    private String selectedProtocol;
    private final SSLEngine engine;

    SpdyServerProvider(SSLEngine engine) {
        this.engine = engine;
    }

    @Override
    public void unsupported() {
        ALPN.remove(engine);
        selectedProtocol = "http/1.1";
    }

    public SpdyOrHttpChooser.SelectedProtocol getSelectedProtocol() {
        if (selectedProtocol == null) {
            return SpdyOrHttpChooser.SelectedProtocol.UNKNOWN;
        }
        return SpdyOrHttpChooser.SelectedProtocol.protocol(selectedProtocol);
    }

    @Override
    public String select(List<String> protocols) {
        
        
        for (String protocol : protocols) {
            if (protocol.contains("spdy")) {
                selectedProtocol = protocol;
                return selectedProtocol;
            }
        }
        
        for (String protocol : protocols) {
            if (protocol.contains("http")) {
                selectedProtocol = protocol;
                return selectedProtocol;
            }
        }
        
        selectedProtocol = protocols.get(0);
        return selectedProtocol;
    }
}
