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

package io.xlibb.solace.observability;

/**
 * Constants for Solace observability (metrics and tracing).
 */
public class SolaceObservabilityConstants {

    static final String CONNECTOR_NAME = "solace";

    static final String[] METRIC_PUBLISHERS = {"publishers", "Number of currently active publishers"};
    static final String[] METRIC_CONSUMERS = {"consumers", "Number of currently active consumers"};
    static final String[] METRIC_PUBLISHED = {"published", "Number of messages published"};
    static final String[] METRIC_PUBLISHED_SIZE = {"published_size", "Total size in bytes of messages published"};
    static final String[] METRIC_CONSUMED = {"consumed", "Number of messages consumed"};
    static final String[] METRIC_CONSUMED_SIZE = {"consumed_size", "Total size in bytes of messages consumed"};
    static final String[] METRIC_ERRORS = {"errors", "Number of errors"};

    static final String TAG_URL = "url";
    static final String TAG_DESTINATION = "destination";
    static final String TAG_ERROR_TYPE = "error_type";
    static final String TAG_CONTEXT = "context";
    static final String TAG_LISTENER_NAME = "listener.name";

    public static final String ERROR_TYPE_CONNECTION = "connection";
    public static final String ERROR_TYPE_PUBLISH = "publish";
    public static final String ERROR_TYPE_CLOSE = "close";
    public static final String ERROR_TYPE_RECEIVE = "receive";
    public static final String ERROR_TYPE_ACKNOWLEDGE = "acknowledge";
    public static final String ERROR_TYPE_NACK = "nack";
    public static final String ERROR_TYPE_COMMIT = "commit";
    public static final String ERROR_TYPE_ROLLBACK = "rollback";

    public static final String CONTEXT_PRODUCER = "producer";
    public static final String CONTEXT_CONSUMER = "consumer";

    public static final String UNKNOWN = "unknown";

    private SolaceObservabilityConstants() {
    }
}
