package io.ballerina.lib.solace.smf.consumer;

import com.solacesystems.jcsmp.BytesXMLMessage;
import com.solacesystems.jcsmp.ConsumerFlowProperties;
import com.solacesystems.jcsmp.DurableTopicEndpoint;
import com.solacesystems.jcsmp.EndpointProperties;
import com.solacesystems.jcsmp.FlowReceiver;
import com.solacesystems.jcsmp.JCSMPException;
import com.solacesystems.jcsmp.JCSMPFactory;
import com.solacesystems.jcsmp.JCSMPProperties;
import com.solacesystems.jcsmp.JCSMPSession;
import com.solacesystems.jcsmp.Queue;
import com.solacesystems.jcsmp.Topic;
import com.solacesystems.jcsmp.XMLMessage;
import com.solacesystems.jcsmp.XMLMessageConsumer;
import com.solacesystems.jcsmp.transaction.TransactedSession;
import io.ballerina.lib.solace.smf.common.CommonUtils;
import io.ballerina.lib.solace.smf.config.ConfigurationUtils;
import io.ballerina.lib.solace.smf.config.ConsumerConfiguration;
import io.ballerina.lib.solace.smf.config.ConsumerSubscriptionConfig;
import io.ballerina.lib.solace.smf.config.QueueConsumerConfig;
import io.ballerina.lib.solace.smf.config.TopicConsumerConfig;
import io.ballerina.runtime.api.values.BDecimal;
import io.ballerina.runtime.api.values.BError;
import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BObject;
import io.ballerina.runtime.api.values.BString;

import java.math.BigDecimal;

/**
 * Consumer actions - main entry point for Ballerina MessageConsumer interop.
 */
public class ConsumerActions {

    private static final String NATIVE_SESSION = "native.session";
    private static final String NATIVE_TX_SESSION = "native.tx.session";
    private static final String NATIVE_FLOW = "native.flow";
    private static final String NATIVE_CONSUMER = "native.consumer";
    private static final String NATIVE_SUBSCRIPTION_TYPE = "native.subscription.type";
    private static final String NATIVE_TRANSACTED = "native.transacted";
    private static final String NATIVE_CLOSED = "native.closed";

    private static final String SUBSCRIPTION_TYPE_QUEUE = "QUEUE";
    private static final String SUBSCRIPTION_TYPE_DIRECT_TOPIC = "DIRECT_TOPIC";
    private static final String SUBSCRIPTION_TYPE_DURABLE_TOPIC = "DURABLE_TOPIC";

    /**
     * Functional interface for creating a FlowReceiver from flow properties.
     * This allows abstracting the difference between JCSMPSession and TransactedSession.
     */
    @FunctionalInterface
    private interface FlowReceiverFactory {
        FlowReceiver createFlow(ConsumerFlowProperties flowProps) throws JCSMPException;
    }

    /**
     * Initialize the consumer with connection URL and configuration. Creates either a transacted or non-transacted
     * consumer based on configuration.
     *
     * @param consumer the Ballerina consumer object
     * @param url      the broker URL
     * @param config   the consumer configuration
     * @return null on success, BError on failure
     */
    public static BError init(BObject consumer, BString url, BMap<BString, Object> config) {
        try {
            // Parse configuration
            ConsumerConfiguration consumerConfig = new ConsumerConfiguration(config);
            ConsumerSubscriptionConfig subscriptionConfig = consumerConfig.subscriptionConfig();
            boolean isTransacted = consumerConfig.connectionConfig().transacted();

            // Build JCSMP properties from configuration
            JCSMPProperties jcsmpProps =
                    ConfigurationUtils.buildJCSMPProperties(url.getValue(), consumerConfig.connectionConfig());

            // Create and connect base JCSMP session
            final JCSMPSession session = JCSMPFactory.onlyInstance().createSession(jcsmpProps);
            session.connect();

            // Validate: Direct topic subscriptions cannot be transacted
            if (isTransacted && subscriptionConfig instanceof TopicConsumerConfig topicConfig &&
                    !topicConfig.isDurable()) {
                return CommonUtils.createError("Transacted mode is not supported for direct topic subscriptions. " +
                        "Use DURABLE endpoint type for guaranteed delivery with transactions.");
            }

            // Create TransactedSession if in transacted mode
            final TransactedSession txSession = isTransacted ? session.createTransactedSession() : null;

            // Store session references and transacted flag
            consumer.addNativeData(NATIVE_SESSION, session);
            consumer.addNativeData(NATIVE_TX_SESSION, txSession);
            consumer.addNativeData(NATIVE_TRANSACTED, isTransacted);
            consumer.addNativeData(NATIVE_CLOSED, false);

            // Create appropriate consumer based on subscription type
            if (subscriptionConfig instanceof QueueConsumerConfig queueConfig) {
                FlowReceiverFactory factory = isTransacted
                        ? props -> txSession.createFlow(null, props, null)
                        : props -> session.createFlow(null, props);
                createQueueConsumer(consumer, factory, queueConfig, isTransacted);
            } else if (subscriptionConfig instanceof TopicConsumerConfig topicConfig) {
                topicConfig.validate();
                if (topicConfig.isDurable()) {
                    FlowReceiverFactory factory = isTransacted
                            ? props -> txSession.createFlow(null, props, null)
                            : props -> session.createFlow(null, props);
                    createDurableTopicConsumer(consumer, factory, topicConfig, isTransacted);
                } else {
                    // Direct topic - always non-transacted (already validated above)
                    createDirectTopicConsumer(consumer, session, topicConfig);
                }
            } else {
                return CommonUtils.createError("Unknown subscription configuration type");
            }

            return null;
        } catch (Exception e) {
            return CommonUtils.createError("Failed to initialize consumer", e);
        }
    }

    /**
     * Configures common flow properties from a consumer subscription config.
     * Applies to both queue and durable topic consumers.
     *
     * @param flowProps the flow properties to configure
     * @param config    the consumer subscription configuration containing common fields
     */
    private static void configureFlowProperties(ConsumerFlowProperties flowProps,
                                                ConsumerSubscriptionConfig config) {
        // Set ack mode
        flowProps.setAckMode(config.ackMode());

        // Set selector if present
        if (config.selector() != null) {
            flowProps.setSelector(config.selector());
        }

        // Set optional flow control properties
        if (config.transportWindowSize() != null) {
            flowProps.setTransportWindowSize(config.transportWindowSize());
        }
        if (config.ackThreshold() != null) {
            flowProps.setAckThreshold(config.ackThreshold());
        }
        if (config.ackTimerInMsecs() != null) {
            flowProps.setAckTimerInMsecs(config.ackTimerInMsecs());
        }
        if (config.startState() != null) {
            flowProps.setStartState(config.startState());
        }
        if (config.noLocal() != null) {
            flowProps.setNoLocal(config.noLocal());
        }
        if (config.activeFlowIndication() != null) {
            flowProps.setActiveFlowIndication(config.activeFlowIndication());
        }
        if (config.reconnectTries() != null) {
            flowProps.setReconnectTries(config.reconnectTries());
        }
        if (config.reconnectRetryIntervalInMsecs() != null) {
            flowProps.setReconnectRetryIntervalInMsecs(config.reconnectRetryIntervalInMsecs());
        }
    }

    /**
     * Creates a queue for consumption (temporary or regular).
     *
     * @param session the JCSMP session
     * @param config  the queue consumer configuration
     * @return the created Queue
     * @throws JCSMPException if queue creation fails
     */
    private static Queue createQueue(JCSMPSession session, QueueConsumerConfig config) throws JCSMPException {
        if (config.temporary()) {
            return (config.queueName() != null && !config.queueName().isEmpty())
                    ? session.createTemporaryQueue(config.queueName())
                    : session.createTemporaryQueue();
        }
        return JCSMPFactory.onlyInstance().createQueue(config.queueName());
    }

    /**
     * Creates a FlowReceiver for queue consumption (unified for transacted/non-transacted).
     *
     * @param consumer       the Ballerina consumer object
     * @param flowFactory    the factory function for creating the flow receiver
     * @param config         the queue consumer configuration
     * @param isTransacted   whether this is a transacted flow
     * @throws Exception if flow creation fails
     */
    private static void createQueueConsumer(BObject consumer, FlowReceiverFactory flowFactory,
                                           QueueConsumerConfig config, boolean isTransacted) throws Exception {
        JCSMPSession baseSession = (JCSMPSession) consumer.getNativeData(NATIVE_SESSION);
        Queue queue = createQueue(baseSession, config);

        ConsumerFlowProperties flowProps = new ConsumerFlowProperties();
        flowProps.setEndpoint(queue);
        configureFlowProperties(flowProps, config);

        // Add settlement outcomes only for non-transacted flows
        if (!isTransacted) {
            flowProps.addRequiredSettlementOutcomes(XMLMessage.Outcome.FAILED, XMLMessage.Outcome.REJECTED);
        }

        // Create flow using the factory function
        FlowReceiver flowReceiver = flowFactory.createFlow(flowProps);
        flowReceiver.start();

        consumer.addNativeData(NATIVE_FLOW, flowReceiver);
        consumer.addNativeData(NATIVE_SUBSCRIPTION_TYPE, SUBSCRIPTION_TYPE_QUEUE);
    }

    /**
     * Creates a FlowReceiver for durable topic subscription (unified for transacted/non-transacted).
     *
     * @param consumer       the Ballerina consumer object
     * @param flowFactory    the factory function for creating the flow receiver
     * @param config         the topic consumer configuration
     * @param isTransacted   whether this is a transacted flow
     * @throws Exception if flow creation fails
     */
    private static void createDurableTopicConsumer(BObject consumer, FlowReceiverFactory flowFactory,
                                                   TopicConsumerConfig config, boolean isTransacted) throws Exception {
        // Note: Provisioning requires base JCSMPSession (cannot provision on TransactedSession)
        JCSMPSession baseSession = (JCSMPSession) consumer.getNativeData(NATIVE_SESSION);

        // Create durable topic endpoint
        DurableTopicEndpoint endpoint = JCSMPFactory.onlyInstance().createDurableTopicEndpoint(config.endpointName());

        // Create topic subscription
        Topic topic = JCSMPFactory.onlyInstance().createTopic(config.topicName());

        // Provision the endpoint (ignore if already exists)
        EndpointProperties endpointProps = new EndpointProperties();
        baseSession.provision(endpoint, endpointProps, JCSMPSession.FLAG_IGNORE_ALREADY_EXISTS);

        ConsumerFlowProperties flowProps = new ConsumerFlowProperties();
        flowProps.setEndpoint(endpoint);
        flowProps.setNewSubscription(topic);
        configureFlowProperties(flowProps, config);

        // Add settlement outcomes only for non-transacted flows
        if (!isTransacted) {
            flowProps.addRequiredSettlementOutcomes(XMLMessage.Outcome.FAILED, XMLMessage.Outcome.REJECTED);
        }

        // Create flow using the factory function
        FlowReceiver flowReceiver = flowFactory.createFlow(flowProps);
        flowReceiver.start();

        consumer.addNativeData(NATIVE_FLOW, flowReceiver);
        consumer.addNativeData(NATIVE_SUBSCRIPTION_TYPE, SUBSCRIPTION_TYPE_DURABLE_TOPIC);
    }

    /**
     * Creates an XMLMessageConsumer for direct topic subscription.
     */
    private static void createDirectTopicConsumer(BObject consumer, JCSMPSession session, TopicConsumerConfig config)
            throws Exception {
        Topic topic = JCSMPFactory.onlyInstance().createTopic(config.topicName());

        // Create consumer without listener (for sync receive)
        XMLMessageConsumer xmlConsumer = session.getMessageConsumer((com.solacesystems.jcsmp.XMLMessageListener) null);

        // Add subscription
        session.addSubscription(topic);

        // Start consumer
        xmlConsumer.start();

        consumer.addNativeData(NATIVE_CONSUMER, xmlConsumer);
        consumer.addNativeData(NATIVE_SUBSCRIPTION_TYPE, SUBSCRIPTION_TYPE_DIRECT_TOPIC);
    }

    /**
     * Receive a message with timeout.
     *
     * @param consumer the Ballerina consumer object
     * @param timeout  the timeout in seconds
     * @return the received message, null if timeout, or BError on failure
     */
    public static Object receive(BObject consumer, BDecimal timeout) {
        try {
            Boolean closed = (Boolean) consumer.getNativeData(NATIVE_CLOSED);
            if (closed != null && closed) {
                return CommonUtils.createError("Consumer is closed");
            }

            long timeoutMs = timeout.decimalValue().multiply(BigDecimal.valueOf(1000)).longValue();
            String subscriptionType = (String) consumer.getNativeData(NATIVE_SUBSCRIPTION_TYPE);

            BytesXMLMessage message = null;

            if (SUBSCRIPTION_TYPE_QUEUE.equals(subscriptionType) ||
                    SUBSCRIPTION_TYPE_DURABLE_TOPIC.equals(subscriptionType)) {
                FlowReceiver flowReceiver = (FlowReceiver) consumer.getNativeData(NATIVE_FLOW);
                if (flowReceiver == null) {
                    return CommonUtils.createError("Consumer flow not initialized");
                }
                message = flowReceiver.receive((int) timeoutMs);
            } else if (SUBSCRIPTION_TYPE_DIRECT_TOPIC.equals(subscriptionType)) {
                XMLMessageConsumer xmlConsumer = (XMLMessageConsumer) consumer.getNativeData(NATIVE_CONSUMER);
                if (xmlConsumer == null) {
                    return CommonUtils.createError("Consumer not initialized");
                }
                message = xmlConsumer.receive((int) timeoutMs);
            }

            if (message == null) {
                return null; // Timeout - no message available
            }

            // Convert to Ballerina Message
            return MessageConverter.toBallerinaMessage(message);
        } catch (Exception e) {
            return CommonUtils.createError("Failed to receive message", e);
        }
    }

    /**
     * Receive a message without waiting.
     *
     * @param consumer the Ballerina consumer object
     * @return the received message, null if none available, or BError on failure
     */
    public static Object receiveNoWait(BObject consumer) {
        try {
            Boolean closed = (Boolean) consumer.getNativeData(NATIVE_CLOSED);
            if (closed != null && closed) {
                return CommonUtils.createError("Consumer is closed");
            }

            String subscriptionType = (String) consumer.getNativeData(NATIVE_SUBSCRIPTION_TYPE);

            BytesXMLMessage message = null;

            if (SUBSCRIPTION_TYPE_QUEUE.equals(subscriptionType) ||
                    SUBSCRIPTION_TYPE_DURABLE_TOPIC.equals(subscriptionType)) {
                FlowReceiver flowReceiver = (FlowReceiver) consumer.getNativeData(NATIVE_FLOW);
                if (flowReceiver == null) {
                    return CommonUtils.createError("Consumer flow not initialized");
                }
                message = flowReceiver.receiveNoWait();
            } else if (SUBSCRIPTION_TYPE_DIRECT_TOPIC.equals(subscriptionType)) {
                XMLMessageConsumer xmlConsumer = (XMLMessageConsumer) consumer.getNativeData(NATIVE_CONSUMER);
                if (xmlConsumer == null) {
                    return CommonUtils.createError("Consumer not initialized");
                }
                message = xmlConsumer.receiveNoWait();
            }

            if (message == null) {
                return null; // No message available
            }

            // Convert to Ballerina Message
            return MessageConverter.toBallerinaMessage(message);
        } catch (Exception e) {
            return CommonUtils.createError("Failed to receive message", e);
        }
    }

    /**
     * Acknowledge a message.
     *
     * @param consumer the Ballerina consumer object
     * @param message  the Ballerina message to acknowledge
     * @return null on success, BError on failure
     */
    public static BError acknowledge(BObject consumer, BMap<BString, Object> message) {
        try {
            Boolean closed = (Boolean) consumer.getNativeData(NATIVE_CLOSED);
            if (closed != null && closed) {
                return CommonUtils.createError("Consumer is closed");
            }

            XMLMessage nativeMessage = MessageConverter.extractNativeMessage(message);
            if (nativeMessage == null) {
                return CommonUtils.createError("Cannot acknowledge: native message not found");
            }

            nativeMessage.ackMessage();
            return null;
        } catch (Exception e) {
            return CommonUtils.createError("Failed to acknowledge message", e);
        }
    }

    /**
     * Negatively acknowledge a message (NACK).
     *
     * @param consumer the Ballerina consumer object
     * @param message  the Ballerina message to NACK
     * @param requeue  if true, use FAILED outcome (requeue); if false, use REJECTED outcome (DMQ)
     * @return null on success, BError on failure
     */
    public static BError nack(BObject consumer, BMap<BString, Object> message, boolean requeue) {
        try {
            Boolean closed = (Boolean) consumer.getNativeData(NATIVE_CLOSED);
            if (closed != null && closed) {
                return CommonUtils.createError("Consumer is closed");
            }

            XMLMessage nativeMessage = MessageConverter.extractNativeMessage(message);
            if (nativeMessage == null) {
                return CommonUtils.createError("Cannot NACK: native message not found");
            }

            // Use settle() with appropriate outcome
            XMLMessage.Outcome outcome = requeue ? XMLMessage.Outcome.FAILED : XMLMessage.Outcome.REJECTED;
            nativeMessage.settle(outcome);
            return null;
        } catch (Exception e) {
            return CommonUtils.createError("Failed to NACK message", e);
        }
    }

    /**
     * Commit the current transaction. Only valid for transacted consumers (when connectionConfig.transacted = true).
     *
     * @param consumer the Ballerina consumer object
     * @return null on success, BError on failure
     */
    public static BError commit(BObject consumer) {
        try {
            Boolean closed = (Boolean) consumer.getNativeData(NATIVE_CLOSED);
            if (closed != null && closed) {
                return CommonUtils.createError("Consumer is closed");
            }

            Boolean transacted = (Boolean) consumer.getNativeData(NATIVE_TRANSACTED);
            if (transacted == null || !transacted) {
                return CommonUtils.createError("commit() can only be called on transacted consumers. " +
                        "Set connectionConfig.transacted = true to enable transactions.");
            }

            TransactedSession txSession = (TransactedSession) consumer.getNativeData(NATIVE_TX_SESSION);
            if (txSession == null) {
                return CommonUtils.createError("TransactedSession not initialized");
            }

            // Commit transaction on TransactedSession (blocking operation)
            Object result = CommonUtils.executeBlocking(txSession::commit);

            if (result instanceof BError) {
                return (BError) result;
            }

            return null;
        } catch (Exception e) {
            return CommonUtils.createError("Failed to commit transaction", e);
        }
    }

    /**
     * Rollback the current transaction. Only valid for transacted consumers (when connectionConfig.transacted = true).
     *
     * @param consumer the Ballerina consumer object
     * @return null on success, BError on failure
     */
    public static BError rollback(BObject consumer) {
        try {
            Boolean closed = (Boolean) consumer.getNativeData(NATIVE_CLOSED);
            if (closed != null && closed) {
                return CommonUtils.createError("Consumer is closed");
            }

            Boolean transacted = (Boolean) consumer.getNativeData(NATIVE_TRANSACTED);
            if (transacted == null || !transacted) {
                return CommonUtils.createError("rollback() can only be called on transacted consumers. " +
                        "Set connectionConfig.transacted = true to enable transactions.");
            }

            TransactedSession txSession = (TransactedSession) consumer.getNativeData(NATIVE_TX_SESSION);
            if (txSession == null) {
                return CommonUtils.createError("TransactedSession not initialized");
            }

            // Rollback transaction on TransactedSession (blocking operation)
            Object result = CommonUtils.executeBlocking(txSession::rollback);

            if (result instanceof BError) {
                return (BError) result;
            }

            return null;
        } catch (Exception e) {
            return CommonUtils.createError("Failed to rollback transaction", e);
        }
    }

    /**
     * Close the consumer and release resources. Closes flow receiver/consumer, transacted session (if any), and base
     * session in that order.
     *
     * @param consumer the Ballerina consumer object
     * @return null on success, BError on failure
     */
    public static BError close(BObject consumer) {
        try {
            String subscriptionType = (String) consumer.getNativeData(NATIVE_SUBSCRIPTION_TYPE);

            // Close flow receiver or XML consumer
            if (SUBSCRIPTION_TYPE_QUEUE.equals(subscriptionType) ||
                    SUBSCRIPTION_TYPE_DURABLE_TOPIC.equals(subscriptionType)) {
                FlowReceiver flowReceiver = (FlowReceiver) consumer.getNativeData(NATIVE_FLOW);
                if (flowReceiver != null) {
                    try {
                        flowReceiver.stop();
                        flowReceiver.close();
                    } catch (Exception e) {
                        // Log but continue
                    }
                }
            } else if (SUBSCRIPTION_TYPE_DIRECT_TOPIC.equals(subscriptionType)) {
                XMLMessageConsumer xmlConsumer = (XMLMessageConsumer) consumer.getNativeData(NATIVE_CONSUMER);
                if (xmlConsumer != null) {
                    try {
                        xmlConsumer.stop();
                        xmlConsumer.close();
                    } catch (Exception e) {
                        // Log but continue
                    }
                }
            }

            // Close transacted session if present
            TransactedSession txSession = (TransactedSession) consumer.getNativeData(NATIVE_TX_SESSION);
            if (txSession != null) {
                try {
                    txSession.close();
                } catch (Exception e) {
                    // Log but continue
                }
            }

            // Close base session
            JCSMPSession session = (JCSMPSession) consumer.getNativeData(NATIVE_SESSION);
            if (session != null) {
                try {
                    session.closeSession();
                } catch (Exception e) {
                    // Log but continue
                }
            }

            // Mark as closed and clear native data
            consumer.addNativeData(NATIVE_CLOSED, true);
            consumer.addNativeData(NATIVE_FLOW, null);
            consumer.addNativeData(NATIVE_CONSUMER, null);
            consumer.addNativeData(NATIVE_TX_SESSION, null);
            consumer.addNativeData(NATIVE_TRANSACTED, null);
            consumer.addNativeData(NATIVE_SESSION, null);

            return null;
        } catch (Exception e) {
            return CommonUtils.createError("Failed to close consumer", e);
        }
    }
}
