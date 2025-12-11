package io.ballerina.lib.solace.smf.consumer;

import com.solacesystems.jcsmp.ConsumerFlowProperties;
import com.solacesystems.jcsmp.DurableTopicEndpoint;
import com.solacesystems.jcsmp.EndpointProperties;
import com.solacesystems.jcsmp.FlowReceiver;
import com.solacesystems.jcsmp.JCSMPException;
import com.solacesystems.jcsmp.JCSMPFactory;
import com.solacesystems.jcsmp.JCSMPSession;
import com.solacesystems.jcsmp.Queue;
import com.solacesystems.jcsmp.Topic;
import com.solacesystems.jcsmp.XMLMessage;
import com.solacesystems.jcsmp.XMLMessageConsumer;
import io.ballerina.lib.solace.smf.config.ConsumerSubscriptionConfig;
import io.ballerina.lib.solace.smf.config.QueueConsumerConfig;
import io.ballerina.lib.solace.smf.config.TopicConsumerConfig;
import io.ballerina.runtime.api.values.BObject;

import static io.ballerina.lib.solace.smf.common.Constants.NATIVE_CONSUMER;
import static io.ballerina.lib.solace.smf.common.Constants.NATIVE_FLOW;
import static io.ballerina.lib.solace.smf.common.Constants.NATIVE_SESSION;
import static io.ballerina.lib.solace.smf.common.Constants.NATIVE_SUBSCRIPTION_TYPE;

/**
 * Utility class for consumer-related operations.
 */
public class ConsumerUtils {

    public static final String SUBSCRIPTION_TYPE_QUEUE = "QUEUE";
    public static final String SUBSCRIPTION_TYPE_DIRECT_TOPIC = "DIRECT_TOPIC";
    public static final String SUBSCRIPTION_TYPE_DURABLE_TOPIC = "DURABLE_TOPIC";

    /**
     * Configures common flow properties from a consumer subscription config. Applies to both queue and durable topic
     * consumers.
     *
     * @param flowProps the flow properties to configure
     * @param config    the consumer subscription configuration containing common fields
     */
    public static void configureFlowProperties(ConsumerFlowProperties flowProps,
                                               ConsumerSubscriptionConfig config) {
        flowProps.setAckMode(config.ackMode().getJcsmpMode());

        if (config.selector() != null) {
            flowProps.setSelector(config.selector());
        }

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
     * Creates a FlowReceiver for queue consumption
     *
     * @param consumer     the Ballerina consumer object
     * @param flowFactory  the factory function for creating the flow receiver
     * @param config       the queue consumer configuration
     * @param isTransacted whether this is a transacted flow
     * @throws Exception if flow creation fails
     */
    public static void createQueueConsumer(BObject consumer, FlowReceiverFactory flowFactory,
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
     * Creates a FlowReceiver for durable topic subscription
     *
     * @param consumer     the Ballerina consumer object
     * @param flowFactory  the factory function for creating the flow receiver
     * @param config       the topic consumer configuration
     * @param isTransacted whether this is a transacted flow
     * @throws Exception if flow creation fails
     */
    public static void createDurableTopicConsumer(BObject consumer, FlowReceiverFactory flowFactory,
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
    public static void createDirectTopicConsumer(BObject consumer, JCSMPSession session, TopicConsumerConfig config)
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
}
