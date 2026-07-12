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

package io.ballerina.lib.solace.listener;

import com.solacesystems.jcsmp.ConsumerFlowProperties;
import com.solacesystems.jcsmp.DurableTopicEndpoint;
import com.solacesystems.jcsmp.EndpointProperties;
import com.solacesystems.jcsmp.FlowReceiver;
import com.solacesystems.jcsmp.JCSMPFactory;
import com.solacesystems.jcsmp.JCSMPProperties;
import com.solacesystems.jcsmp.JCSMPSession;
import com.solacesystems.jcsmp.Queue;
import com.solacesystems.jcsmp.Topic;
import com.solacesystems.jcsmp.XMLMessage;
import com.solacesystems.jcsmp.XMLMessageConsumer;
import com.solacesystems.jcsmp.transaction.TransactedSession;
import io.ballerina.lib.solace.ModuleUtils;
import io.ballerina.lib.solace.common.CommonUtils;
import io.ballerina.lib.solace.config.ConfigurationUtils;
import io.ballerina.lib.solace.config.ConsumerSubscriptionConfig;
import io.ballerina.lib.solace.config.ListenerConfiguration;
import io.ballerina.lib.solace.config.QueueConsumerConfig;
import io.ballerina.lib.solace.config.TopicConsumerConfig;
import io.ballerina.lib.solace.consumer.AcknowledgementMode;
import io.ballerina.lib.solace.consumer.ConsumerUtils;
import io.ballerina.runtime.api.Environment;
import io.ballerina.runtime.api.Runtime;
import io.ballerina.runtime.api.creators.ValueCreator;
import io.ballerina.runtime.api.values.BError;
import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BObject;
import io.ballerina.runtime.api.values.BString;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static io.ballerina.lib.solace.common.Constants.NATIVE_CLOSED;
import static io.ballerina.lib.solace.common.Constants.NATIVE_RUNTIME;
import static io.ballerina.lib.solace.common.Constants.NATIVE_SERVICES;
import static io.ballerina.lib.solace.common.Constants.NATIVE_SESSION;
import static io.ballerina.lib.solace.common.Constants.NATIVE_STARTED;
import static io.ballerina.lib.solace.common.Constants.NATIVE_TRANSACTED;
import static io.ballerina.lib.solace.common.Constants.NATIVE_TX_SESSION;
import static io.ballerina.lib.solace.common.Constants.NATIVE_URL;
import static io.ballerina.lib.solace.consumer.ConsumerUtils.SUBSCRIPTION_TYPE_DIRECT_TOPIC;
import static io.ballerina.lib.solace.consumer.ConsumerUtils.SUBSCRIPTION_TYPE_DURABLE_TOPIC;
import static io.ballerina.lib.solace.consumer.ConsumerUtils.SUBSCRIPTION_TYPE_QUEUE;

/**
 * Listener actions - entry point for the Ballerina Solace {@code Listener} interop. Manages a JCSMP session and a set
 * of attached services, each backed by an asynchronous {@link FlowReceiver} (queue / durable topic endpoint) or an
 * {@link XMLMessageConsumer} (direct topic) that pushes messages into the service via {@link SolaceMessageListener}.
 */
public class ListenerActions {

    /**
     * Initialize the listener: create and connect the JCSMP session (and a transacted session if requested).
     *
     * @param env      the Ballerina environment (used to capture the runtime for service callbacks)
     * @param listener the Ballerina listener object
     * @param url      the broker URL
     * @param config   the connection configuration
     * @return null on success, BError on failure
     */
    public static Object init(Environment env, BObject listener, BString url, BMap<BString, Object> config) {
        try {
            ListenerConfiguration listenerConfig = new ListenerConfiguration(config);
            JCSMPProperties props =
                    ConfigurationUtils.buildJCSMPProperties(url.getValue(), listenerConfig.connectionConfig());
            ConfigurationUtils.applyReceiveTimestampProperty(props, listenerConfig.generateReceiveTimestamps());

            JCSMPSession session = JCSMPFactory.onlyInstance().createSession(props);
            session.connect();

            boolean isTransacted = listenerConfig.connectionConfig().transacted();
            TransactedSession txSession = isTransacted ? session.createTransactedSession() : null;

            listener.addNativeData(NATIVE_SESSION, session);
            listener.addNativeData(NATIVE_TX_SESSION, txSession);
            listener.addNativeData(NATIVE_TRANSACTED, isTransacted);
            listener.addNativeData(NATIVE_CLOSED, false);
            listener.addNativeData(NATIVE_STARTED, false);
            listener.addNativeData(NATIVE_URL, url.getValue());
            listener.addNativeData(NATIVE_RUNTIME, env.getRuntime());
            // Concurrent map: attach()/detach() run concurrently with start()/gracefulStop()/immediateStop()
            // iterating this map (dynamic attach after start is supported), so a plain HashMap/LinkedHashMap
            // would risk a ConcurrentModificationException or corrupting the map.
            listener.addNativeData(NATIVE_SERVICES, new ConcurrentHashMap<BObject, AttachedService>());
            return null;
        } catch (Exception e) {
            return CommonUtils.createError("Failed to initialize listener", e);
        }
    }

    /**
     * Attach a service to the listener. Reads the {@code @solace:ServiceConfig} annotation, validates the service's
     * {@code onMessage} method, and creates the backing receiver. If the listener is already started, delivery to the
     * newly attached service begins immediately.
     *
     * @param listener the Ballerina listener object
     * @param service  the Ballerina service object
     * @param name     optional service name (unused; subscription comes from the annotation)
     * @return null on success, BError on failure
     */
    public static Object attach(BObject listener, BObject service, Object name) {
        try {
            if (isClosed(listener)) {
                return CommonUtils.createError("Listener is closed");
            }

            Runtime runtime = (Runtime) listener.getNativeData(NATIVE_RUNTIME);
            Service.validateService(runtime, service);
            Service nativeService = new Service(service);

            BMap<BString, Object> serviceConfig = Service.getServiceConfigAnnotation(service);
            ConsumerSubscriptionConfig subscriptionConfig = ConsumerSubscriptionConfig.fromBMap(serviceConfig);
            subscriptionConfig.validate();
            boolean isTransacted = (Boolean) listener.getNativeData(NATIVE_TRANSACTED);

            // On a transacted listener, settlement only happens via caller->commit()/rollback() on the shared
            // transacted session; AUTO_ACK would call message.ackMessage(), which is a no-op on a transacted
            // flow, so messages would never actually be committed and would redeliver indefinitely.
            if (isTransacted && subscriptionConfig.ackMode() == AcknowledgementMode.AUTO_ACK) {
                return CommonUtils.createError(
                        "AUTO_ACK is not supported on a transacted listener; message settlement must be driven "
                                + "explicitly via caller->commit()/caller->rollback(). Set ackMode: "
                                + "solace:CLIENT_ACK on the service configuration.");
            }

            if (subscriptionConfig instanceof TopicConsumerConfig topicConfig) {
                if (isTransacted && !topicConfig.isDurable()) {
                    return CommonUtils.createError("Transacted mode is not supported for direct topic subscriptions. "
                            + "Use DURABLE endpoint type for guaranteed delivery with transactions.");
                }
                if (!topicConfig.isDurable() && hasDirectTopicService(listener)) {
                    return CommonUtils.createError("Only one direct topic service can be attached per listener. "
                            + "Use a separate listener or a durable topic endpoint.");
                }
            }

            // Direct topic messages are not guaranteed and carry no acknowledgement, so auto-settle only
            // applies to flow-based subscriptions (queues and durable topic endpoints).
            boolean directTopic = subscriptionConfig instanceof TopicConsumerConfig topicConfig
                    && !topicConfig.isDurable();
            boolean autoAck = subscriptionConfig.ackMode() == AcknowledgementMode.AUTO_ACK && !directTopic;

            JCSMPSession session = (JCSMPSession) listener.getNativeData(NATIVE_SESSION);
            TransactedSession txSession = (TransactedSession) listener.getNativeData(NATIVE_TX_SESSION);

            // Create the Caller supplied to onMessage for explicit ack/nack and transaction control.
            BObject caller = ValueCreator.createObjectValue(ModuleUtils.getModule(), "Caller");
            caller.addNativeData(NATIVE_TX_SESSION, txSession);
            caller.addNativeData(NATIVE_CLOSED, false);

            SolaceMessageListener messageListener =
                    new SolaceMessageListener(runtime, nativeService, caller, autoAck);

            AttachedService attached = createReceiver(session, txSession, isTransacted, subscriptionConfig,
                    messageListener);

            servicesMap(listener).put(service, attached);

            // If the listener is already running, begin delivering to the newly attached service immediately.
            boolean started = (Boolean) listener.getNativeData(NATIVE_STARTED);
            if (started) {
                attached.start();
            }
            return null;
        } catch (BError e) {
            return e;
        } catch (Exception e) {
            return CommonUtils.createError("Failed to attach service", e);
        }
    }

    /**
     * Detach a service: stop and close its receiver and drop it from the listener.
     *
     * @param listener the Ballerina listener object
     * @param service  the Ballerina service object
     * @return null on success, BError on failure
     */
    public static Object detach(BObject listener, BObject service) {
        try {
            AttachedService attached = servicesMap(listener).remove(service);
            if (attached != null) {
                attached.close();
            }
            return null;
        } catch (Exception e) {
            return CommonUtils.createError("Failed to detach service", e);
        }
    }

    /**
     * Start the listener: begin delivery for all attached services.
     *
     * @param listener the Ballerina listener object
     * @return null on success, BError on failure
     */
    public static Object start(BObject listener) {
        try {
            if (isClosed(listener)) {
                return CommonUtils.createError("Listener is closed");
            }
            for (AttachedService attached : servicesMap(listener).values()) {
                attached.start();
            }
            listener.addNativeData(NATIVE_STARTED, true);
            return null;
        } catch (Exception e) {
            return CommonUtils.createError("Failed to start listener", e);
        }
    }

    /**
     * Gracefully stop the listener: pause delivery (letting in-flight processing finish) and release all resources.
     *
     * @param listener the Ballerina listener object
     * @return null on success, BError on failure
     */
    public static Object gracefulStop(BObject listener) {
        return stop(listener, true);
    }

    /**
     * Immediately stop the listener: release all resources without waiting for in-flight processing.
     *
     * @param listener the Ballerina listener object
     * @return null on success, BError on failure
     */
    public static Object immediateStop(BObject listener) {
        return stop(listener, false);
    }

    private static Object stop(BObject listener, boolean graceful) {
        try {
            Map<BObject, AttachedService> services = servicesMap(listener);
            for (AttachedService attached : services.values()) {
                if (graceful) {
                    attached.stop();
                }
                attached.close();
            }
            services.clear();

            TransactedSession txSession = (TransactedSession) listener.getNativeData(NATIVE_TX_SESSION);
            if (txSession != null) {
                txSession.close();
            }
            JCSMPSession session = (JCSMPSession) listener.getNativeData(NATIVE_SESSION);
            if (session != null) {
                session.closeSession();
            }

            listener.addNativeData(NATIVE_STARTED, false);
            listener.addNativeData(NATIVE_CLOSED, true);
            return null;
        } catch (Exception e) {
            return CommonUtils.createError("Failed to stop listener", e);
        }
    }

    private static AttachedService createReceiver(JCSMPSession session, TransactedSession txSession,
                                                  boolean isTransacted, ConsumerSubscriptionConfig subscriptionConfig,
                                                  SolaceMessageListener messageListener)
            throws Exception {
        if (subscriptionConfig instanceof QueueConsumerConfig queueConfig) {
            Queue queue = JCSMPFactory.onlyInstance().createQueue(queueConfig.queueName());
            ConsumerFlowProperties flowProps = new ConsumerFlowProperties();
            flowProps.setEndpoint(queue);
            ConsumerUtils.configureFlowProperties(flowProps, queueConfig);
            if (!isTransacted) {
                flowProps.addRequiredSettlementOutcomes(XMLMessage.Outcome.FAILED, XMLMessage.Outcome.REJECTED);
            }
            FlowReceiver flow = isTransacted
                    ? txSession.createFlow(messageListener, flowProps, null)
                    : session.createFlow(messageListener, flowProps, null);
            return AttachedService.forFlow(SUBSCRIPTION_TYPE_QUEUE, flow, messageListener);
        }

        TopicConsumerConfig topicConfig = (TopicConsumerConfig) subscriptionConfig;
        if (topicConfig.isDurable()) {
            DurableTopicEndpoint endpoint =
                    JCSMPFactory.onlyInstance().createDurableTopicEndpoint(topicConfig.endpointName());
            Topic topic = JCSMPFactory.onlyInstance().createTopic(topicConfig.topicName());
            session.provision(endpoint, new EndpointProperties(), JCSMPSession.FLAG_IGNORE_ALREADY_EXISTS);

            ConsumerFlowProperties flowProps = new ConsumerFlowProperties();
            flowProps.setEndpoint(endpoint);
            flowProps.setNewSubscription(topic);
            ConsumerUtils.configureFlowProperties(flowProps, topicConfig);
            if (!isTransacted) {
                flowProps.addRequiredSettlementOutcomes(XMLMessage.Outcome.FAILED, XMLMessage.Outcome.REJECTED);
            }
            FlowReceiver flow = isTransacted
                    ? txSession.createFlow(messageListener, flowProps, null)
                    : session.createFlow(messageListener, flowProps, null);
            return AttachedService.forFlow(SUBSCRIPTION_TYPE_DURABLE_TOPIC, flow, messageListener);
        }

        // Direct topic: asynchronous XMLMessageConsumer bound to the session.
        Topic topic = JCSMPFactory.onlyInstance().createTopic(topicConfig.topicName());
        XMLMessageConsumer consumer = session.getMessageConsumer(messageListener);
        session.addSubscription(topic);
        return AttachedService.forDirectTopic(consumer, topic, session, messageListener);
    }

    private static boolean hasDirectTopicService(BObject listener) {
        for (AttachedService attached : servicesMap(listener).values()) {
            if (SUBSCRIPTION_TYPE_DIRECT_TOPIC.equals(attached.subscriptionType())) {
                return true;
            }
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    private static Map<BObject, AttachedService> servicesMap(BObject listener) {
        return (Map<BObject, AttachedService>) listener.getNativeData(NATIVE_SERVICES);
    }

    private static boolean isClosed(BObject listener) {
        Boolean closed = (Boolean) listener.getNativeData(NATIVE_CLOSED);
        return closed != null && closed;
    }
}
