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

import com.solacesystems.jcsmp.FlowReceiver;
import com.solacesystems.jcsmp.JCSMPException;
import com.solacesystems.jcsmp.JCSMPSession;
import com.solacesystems.jcsmp.Topic;
import com.solacesystems.jcsmp.XMLMessageConsumer;

import static io.ballerina.lib.solace.consumer.ConsumerUtils.SUBSCRIPTION_TYPE_DIRECT_TOPIC;

/**
 * Holds the native JCSMP receiver and lifecycle state for a single service attached to a {@link ListenerActions}
 * listener. A service is backed either by a {@link FlowReceiver} (queue / durable topic endpoint) or an
 * {@link XMLMessageConsumer} (direct topic).
 */
final class AttachedService {

    private final String subscriptionType;
    private final FlowReceiver flow;
    private final XMLMessageConsumer consumer;
    private final Topic directTopic;
    private final JCSMPSession session;
    private final SolaceMessageListener messageListener;
    private boolean started;

    private AttachedService(String subscriptionType, FlowReceiver flow, XMLMessageConsumer consumer,
                            Topic directTopic, JCSMPSession session, SolaceMessageListener messageListener) {
        this.subscriptionType = subscriptionType;
        this.flow = flow;
        this.consumer = consumer;
        this.directTopic = directTopic;
        this.session = session;
        this.messageListener = messageListener;
    }

    static AttachedService forFlow(String subscriptionType, FlowReceiver flow,
                                   SolaceMessageListener messageListener) {
        return new AttachedService(subscriptionType, flow, null, null, null, messageListener);
    }

    static AttachedService forDirectTopic(XMLMessageConsumer consumer, Topic directTopic, JCSMPSession session,
                                          SolaceMessageListener messageListener) {
        return new AttachedService(SUBSCRIPTION_TYPE_DIRECT_TOPIC, null, consumer, directTopic, session,
                messageListener);
    }

    String subscriptionType() {
        return subscriptionType;
    }

    /**
     * Begins message delivery for this service.
     */
    synchronized void start() throws JCSMPException {
        if (started) {
            return;
        }
        if (flow != null) {
            flow.start();
        } else if (consumer != null) {
            consumer.start();
        }
        started = true;
    }

    /**
     * Pauses message delivery without releasing resources. After {@code stop()} the receiver can be started again.
     */
    synchronized void stop() {
        if (!started) {
            return;
        }
        if (flow != null) {
            flow.stop();
        } else if (consumer != null) {
            consumer.stop();
        }
        started = false;
    }

    /**
     * Stops delivery and releases the receiver and any direct-topic subscription. Does not close the shared session.
     */
    synchronized void close() throws JCSMPException {
        stop();
        // Drain any in-flight service invocation / settlement before releasing the receiver and session, since those
        // tasks may still call ack / commit / rollback on this flow.
        if (messageListener != null) {
            messageListener.shutdown();
        }
        if (flow != null) {
            flow.close();
        }
        if (consumer != null) {
            if (directTopic != null && session != null) {
                session.removeSubscription(directTopic);
            }
            consumer.close();
        }
    }
}
