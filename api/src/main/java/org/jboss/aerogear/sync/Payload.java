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
package org.jboss.aerogear.sync;

/**
 * Represents something that can be exchanged in JSON format.
 *
 * @param <T> the type of the payload
 */
public interface Payload<T> {

    /**
     * Transforms this payload to a JSON String representation.
     *
     * @return {@code String} the payload as a JSON String representation
     */
    String asJson();

    /**
     * Transforms the passed in {@code String} JSON representation into this payloads type.
     *
     * @param json a string representation of this payloads type
     * @return {@code T} an instance of this payloads type
     */
    T fromJson(String json);

}
