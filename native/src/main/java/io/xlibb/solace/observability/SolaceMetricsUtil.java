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

import io.ballerina.runtime.api.values.BObject;
import io.ballerina.runtime.observability.ObserveUtils;
import io.ballerina.runtime.observability.metrics.DefaultMetricRegistry;
import io.ballerina.runtime.observability.metrics.MetricId;
import io.ballerina.runtime.observability.metrics.MetricRegistry;

import static io.xlibb.solace.common.Constants.NATIVE_DESTINATION;
import static io.xlibb.solace.common.Constants.NATIVE_URL;
import static io.xlibb.solace.observability.SolaceObservabilityConstants.CONNECTOR_NAME;
import static io.xlibb.solace.observability.SolaceObservabilityConstants.CONTEXT_CONSUMER;
import static io.xlibb.solace.observability.SolaceObservabilityConstants.CONTEXT_PRODUCER;
import static io.xlibb.solace.observability.SolaceObservabilityConstants.ERROR_TYPE_CONNECTION;
import static io.xlibb.solace.observability.SolaceObservabilityConstants.METRIC_CONSUMERS;
import static io.xlibb.solace.observability.SolaceObservabilityConstants.METRIC_CONSUMED;
import static io.xlibb.solace.observability.SolaceObservabilityConstants.METRIC_CONSUMED_SIZE;
import static io.xlibb.solace.observability.SolaceObservabilityConstants.METRIC_ERRORS;
import static io.xlibb.solace.observability.SolaceObservabilityConstants.METRIC_PUBLISHED;
import static io.xlibb.solace.observability.SolaceObservabilityConstants.METRIC_PUBLISHED_SIZE;
import static io.xlibb.solace.observability.SolaceObservabilityConstants.METRIC_PUBLISHERS;
import static io.xlibb.solace.observability.SolaceObservabilityConstants.TAG_KEY_ERROR_TYPE;
import static io.xlibb.solace.observability.SolaceObservabilityConstants.UNKNOWN;

/**
 * Metrics utility for the Solace connector.
 */
public class SolaceMetricsUtil {

    private static final MetricRegistry metricRegistry = DefaultMetricRegistry.getInstance();

    public static void reportNewProducer(BObject producer) {
        if (!ObserveUtils.isMetricsEnabled()) {
            return;
        }
        SolaceObserverContext ctx = new SolaceObserverContext(CONTEXT_PRODUCER, getUrl(producer));
        incrementGauge(ctx, METRIC_PUBLISHERS[0], METRIC_PUBLISHERS[1]);
    }

    public static void reportNewConsumer(BObject consumer) {
        if (!ObserveUtils.isMetricsEnabled()) {
            return;
        }
        SolaceObserverContext ctx = new SolaceObserverContext(CONTEXT_CONSUMER, getUrl(consumer));
        incrementGauge(ctx, METRIC_CONSUMERS[0], METRIC_CONSUMERS[1]);
    }

    public static void reportProducerClose(BObject producer) {
        if (!ObserveUtils.isMetricsEnabled()) {
            return;
        }
        SolaceObserverContext ctx = new SolaceObserverContext(CONTEXT_PRODUCER, getUrl(producer));
        decrementGauge(ctx, METRIC_PUBLISHERS[0], METRIC_PUBLISHERS[1]);
    }

    public static void reportConsumerClose(BObject consumer) {
        if (!ObserveUtils.isMetricsEnabled()) {
            return;
        }
        SolaceObserverContext ctx = new SolaceObserverContext(CONTEXT_CONSUMER, getUrl(consumer));
        decrementGauge(ctx, METRIC_CONSUMERS[0], METRIC_CONSUMERS[1]);
    }

    public static void reportPublish(BObject producer, String destination, int size) {
        if (!ObserveUtils.isMetricsEnabled()) {
            return;
        }
        SolaceObserverContext ctx = new SolaceObserverContext(CONTEXT_PRODUCER, getUrl(producer), destination);
        incrementCounter(ctx, METRIC_PUBLISHED[0], METRIC_PUBLISHED[1], 1);
        incrementCounter(ctx, METRIC_PUBLISHED_SIZE[0], METRIC_PUBLISHED_SIZE[1], size);
    }

    public static void reportConsume(BObject consumer, int size) {
        if (!ObserveUtils.isMetricsEnabled()) {
            return;
        }
        SolaceObserverContext ctx = new SolaceObserverContext(CONTEXT_CONSUMER, getUrl(consumer),
                getDestination(consumer));
        incrementCounter(ctx, METRIC_CONSUMED[0], METRIC_CONSUMED[1], 1);
        incrementCounter(ctx, METRIC_CONSUMED_SIZE[0], METRIC_CONSUMED_SIZE[1], size);
    }

    public static void reportProducerError(BObject producer, String errorType) {
        if (!ObserveUtils.isMetricsEnabled()) {
            return;
        }
        SolaceObserverContext ctx = new SolaceObserverContext(CONTEXT_PRODUCER, getUrl(producer));
        ctx.addTag(TAG_KEY_ERROR_TYPE, errorType);
        incrementCounter(ctx, METRIC_ERRORS[0], METRIC_ERRORS[1], 1);
    }

    public static void reportProducerError(BObject producer, String destination, String errorType) {
        if (!ObserveUtils.isMetricsEnabled()) {
            return;
        }
        SolaceObserverContext ctx = new SolaceObserverContext(CONTEXT_PRODUCER, getUrl(producer), destination);
        ctx.addTag(TAG_KEY_ERROR_TYPE, errorType);
        incrementCounter(ctx, METRIC_ERRORS[0], METRIC_ERRORS[1], 1);
    }

    public static void reportConsumerError(BObject consumer, String errorType) {
        if (!ObserveUtils.isMetricsEnabled()) {
            return;
        }
        SolaceObserverContext ctx = new SolaceObserverContext(CONTEXT_CONSUMER, getUrl(consumer));
        ctx.addTag(TAG_KEY_ERROR_TYPE, errorType);
        incrementCounter(ctx, METRIC_ERRORS[0], METRIC_ERRORS[1], 1);
    }

    public static void reportConnectionError(String context) {
        if (!ObserveUtils.isMetricsEnabled()) {
            return;
        }
        SolaceObserverContext ctx = new SolaceObserverContext(context);
        ctx.addTag(TAG_KEY_ERROR_TYPE, ERROR_TYPE_CONNECTION);
        incrementCounter(ctx, METRIC_ERRORS[0], METRIC_ERRORS[1], 1);
    }

    static String getUrl(BObject object) {
        Object url = object.getNativeData(NATIVE_URL);
        return url instanceof String ? (String) url : UNKNOWN;
    }

    static String getDestination(BObject object) {
        Object dest = object.getNativeData(NATIVE_DESTINATION);
        return dest instanceof String ? (String) dest : UNKNOWN;
    }

    private static void incrementCounter(SolaceObserverContext ctx, String name, String desc, int amount) {
        if (metricRegistry == null) {
            return;
        }
        metricRegistry.counter(new MetricId(CONNECTOR_NAME + "_" + name, desc, ctx.getAllTags()))
                .increment(amount);
    }

    private static void incrementGauge(SolaceObserverContext ctx, String name, String desc) {
        if (metricRegistry == null) {
            return;
        }
        metricRegistry.gauge(new MetricId(CONNECTOR_NAME + "_" + name, desc, ctx.getAllTags())).increment();
    }

    private static void decrementGauge(SolaceObserverContext ctx, String name, String desc) {
        if (metricRegistry == null) {
            return;
        }
        metricRegistry.gauge(new MetricId(CONNECTOR_NAME + "_" + name, desc, ctx.getAllTags())).decrement();
    }

    private SolaceMetricsUtil() {
    }
}
