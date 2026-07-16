/*
 * Copyright (c) 2026, WSO2 LLC. (http://www.wso2.org).
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

package io.ballerina.lib.solace.observability;

import io.ballerina.runtime.api.Environment;
import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BObject;
import io.ballerina.runtime.api.values.BString;
import io.ballerina.runtime.observability.ObserveUtils;
import io.ballerina.runtime.observability.ObserverContext;

import java.util.HashMap;
import java.util.Map;

import static io.ballerina.lib.solace.common.MessageFieldConstants.PROPERTIES_KEY;
import static io.ballerina.lib.solace.observability.SolaceMetricsUtil.getDestination;
import static io.ballerina.lib.solace.observability.SolaceMetricsUtil.getUrl;
import static io.ballerina.lib.solace.observability.SolaceObservabilityConstants.TAG_KEY_DESTINATION;
import static io.ballerina.lib.solace.observability.SolaceObservabilityConstants.TAG_KEY_URL;

/**
 * Tracing utility for the Solace connector.
 */
public class SolaceTracingUtil {

    // W3C Trace Context header names (https://www.w3.org/TR/trace-context/), carried verbatim as message
    // properties so a publisher's span and a consumer's span can be correlated across the broker hop.
    private static final String TRACEPARENT_KEY = "traceparent";
    private static final String TRACESTATE_KEY = "tracestate";
    private static final String TAG_KEY_UPSTREAM_PREFIX = "upstream.";

    public static void traceResourceInvocation(Environment env, BObject object, String destination) {
        if (!ObserveUtils.isTracingEnabled()) {
            return;
        }
        ObserverContext ctx = ObserveUtils.getObserverContextOfCurrentFrame(env);
        if (ctx == null) {
            ctx = new ObserverContext();
            ObserveUtils.setObserverContextToCurrentFrame(env, ctx);
        }
        ctx.addTag(TAG_KEY_URL, getUrl(object));
        ctx.addTag(TAG_KEY_DESTINATION, destination);
    }

    public static void traceResourceInvocation(Environment env, BObject object) {
        if (!ObserveUtils.isTracingEnabled()) {
            return;
        }
        ObserverContext ctx = ObserveUtils.getObserverContextOfCurrentFrame(env);
        if (ctx == null) {
            ctx = new ObserverContext();
            ObserveUtils.setObserverContextToCurrentFrame(env, ctx);
        }
        ctx.addTag(TAG_KEY_URL, getUrl(object));
        ctx.addTag(TAG_KEY_DESTINATION, getDestination(object));
    }

    /**
     * Returns the current span's context as a W3C traceparent/tracestate carrier, suitable for injecting into an
     * outbound message's properties so a downstream consumer can correlate its trace with this publish.
     *
     * @param env the Ballerina environment of the publishing native call
     * @return the carrier map, or null if tracing is disabled or there is no active span to propagate
     */
    public static Map<String, String> getTraceContextHeaders(Environment env) {
        if (!ObserveUtils.isTracingEnabled()) {
            return null;
        }
        ObserverContext ctx = ObserveUtils.getObserverContextOfCurrentFrame(env);
        if (ctx == null) {
            return null;
        }
        return ObserveUtils.getContextProperties(ctx);
    }

    /**
     * Reads the W3C traceparent/tracestate entries (if any) out of a received Ballerina message's {@code properties}
     * field.
     *
     * @param message the Ballerina message record
     * @return a (possibly empty) carrier map of the trace-context entries found
     */
    @SuppressWarnings("unchecked")
    public static Map<String, String> extractTraceContextHeaders(BMap<BString, Object> message) {
        Map<String, String> carrier = new HashMap<>();
        if (message == null) {
            return carrier;
        }
        Object propsObj = message.get(PROPERTIES_KEY);
        if (!(propsObj instanceof BMap)) {
            return carrier;
        }
        BMap<BString, Object> props = (BMap<BString, Object>) propsObj;
        putIfPresent(carrier, props, TRACEPARENT_KEY);
        putIfPresent(carrier, props, TRACESTATE_KEY);
        return carrier;
    }

    /**
     * Tags the ambient span (of a pull-based {@code receive}/{@code receiveNoWait} client action) with the
     * upstream trace-context carried on the received message. The span for these client actions is already started
     * by the time the native call runs - before the message (and so its trace-context) is known - so a genuine
     * parent-span link isn't possible here; the extracted context is surfaced as tags instead, for manual
     * correlation across the publish/consume boundary.
     *
     * @param env     the Ballerina environment of the receiving native call
     * @param message the received Ballerina message record
     */
    public static void tagUpstreamTraceContext(Environment env, BMap<BString, Object> message) {
        if (!ObserveUtils.isTracingEnabled()) {
            return;
        }
        Map<String, String> carrier = extractTraceContextHeaders(message);
        if (carrier.isEmpty()) {
            return;
        }
        ObserverContext ctx = ObserveUtils.getObserverContextOfCurrentFrame(env);
        if (ctx == null) {
            return;
        }
        carrier.forEach((key, value) -> ctx.addTag(TAG_KEY_UPSTREAM_PREFIX + key, value));
    }

    private static void putIfPresent(Map<String, String> carrier, BMap<BString, Object> props, String key) {
        Object value = props.get(StringUtils.fromString(key));
        if (value != null) {
            carrier.put(key, value.toString());
        }
    }

    private SolaceTracingUtil() {
    }
}
