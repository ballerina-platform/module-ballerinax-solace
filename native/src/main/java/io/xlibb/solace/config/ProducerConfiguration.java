/*
 * Copyright (c) 2025, WSO2 LLC. (http://www.wso2.org).
 *
 *  WSO2 LLC. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied. See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package io.xlibb.solace.config;

import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BString;

/**
 * Producer-specific configuration containing connection configuration. Maps to ProducerConfiguration in Ballerina
 * types.bal.
 *
 * @param connectionConfig connection configuration for broker connection
 */
public record ProducerConfiguration(ConnectionConfiguration connectionConfig) {

    /**
     * Creates a ProducerConfiguration from a Ballerina map record. The map contains connection configuration fields.
     *
     * @param config the Ballerina configuration map
     */
    public ProducerConfiguration(BMap<BString, Object> config) {
        this(new ConnectionConfiguration(config));
    }
}
