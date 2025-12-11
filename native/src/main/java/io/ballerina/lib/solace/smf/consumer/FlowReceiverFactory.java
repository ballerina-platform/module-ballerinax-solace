package io.ballerina.lib.solace.smf.consumer;

import com.solacesystems.jcsmp.ConsumerFlowProperties;
import com.solacesystems.jcsmp.FlowReceiver;
import com.solacesystems.jcsmp.JCSMPException;

/**
 * Functional interface for creating a FlowReceiver from flow properties. This allows abstracting the difference between
 * JCSMPSession and TransactedSession.
 */
@FunctionalInterface
public interface FlowReceiverFactory {

    FlowReceiver createFlow(ConsumerFlowProperties flowProps) throws JCSMPException;
}
