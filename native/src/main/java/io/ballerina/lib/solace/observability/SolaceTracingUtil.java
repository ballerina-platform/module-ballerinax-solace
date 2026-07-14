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
import io.ballerina.runtime.api.values.BObject;
import io.ballerina.runtime.observability.ObserveUtils;
import io.ballerina.runtime.observability.ObserverContext;

import static io.ballerina.lib.solace.observability.SolaceMetricsUtil.getDestination;
import static io.ballerina.lib.solace.observability.SolaceMetricsUtil.getUrl;
import static io.ballerina.lib.solace.observability.SolaceObservabilityConstants.TAG_KEY_DESTINATION;
import static io.ballerina.lib.solace.observability.SolaceObservabilityConstants.TAG_KEY_URL;

/**
 * Tracing utility for the Solace connector.
 */
public class SolaceTracingUtil {

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

    private SolaceTracingUtil() {
    }
}
