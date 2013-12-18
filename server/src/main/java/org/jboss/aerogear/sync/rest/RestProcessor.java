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
package org.jboss.aerogear.sync.rest;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;

/**
 * A implementation of this interface knows how to handle the RESTFul verbs
 * GET, PUT, POST, and DELETE.
 */
public interface RestProcessor {

    /**
     * Handles Http GET requests.
     *
     * @param request The HttpRequest.
     * @param ctx The ChannelHandlerContext.
     *
     * @return {@code HttpResponse} the Http response.
     */
    HttpResponse processGet(HttpRequest request, ChannelHandlerContext ctx);

    /**
     * Handles Http PUT requests.
     *
     * @param request The HttpRequest.
     * @param ctx The ChannelHandlerContext.
     *
     * @return {@code HttpResponse} the Http response.
     */
    HttpResponse processPut(HttpRequest request, ChannelHandlerContext ctx);

    /**
     * Handles Http POST requests.
     *
     * @param request The HttpRequest.
     * @param ctx The ChannelHandlerContext.
     *
     * @return {@code HttpResponse} the Http response.
     */
    HttpResponse processPost(HttpRequest request, ChannelHandlerContext ctx);

    /**
     * Handles Http DELETE requests.
     *
     * @param request The HttpRequest.
     * @param ctx The ChannelHandlerContext.
     *
     * @return {@code HttpResponse} the Http response.
     */
    HttpResponse processDelete(HttpRequest request, ChannelHandlerContext ctx);
}
