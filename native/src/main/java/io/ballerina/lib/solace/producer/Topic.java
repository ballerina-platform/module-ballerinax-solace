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

package io.ballerina.lib.solace.producer;

import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BString;

/**
 * Topic destination for publish/subscribe messaging.
 *
 * @param topicName name of topic to publish messages to
 */
public record Topic(String topicName) implements Destination {
    private static final BString TOPIC_NAME = StringUtils.fromString("topicName");

    /**
     * Creates a Topic from Ballerina configuration map.
     *
     * @param config Ballerina configuration map
     */
    public Topic(BMap<BString, Object> config) {
        this(config.getStringValue(TOPIC_NAME).getValue());
    }
}
