package io.ballerina.lib.solace.smf.producer;

import com.solacesystems.jcsmp.JCSMPSession;
import com.solacesystems.jcsmp.XMLMessageProducer;

/**
 * Wrapper for creating JCSMP message producers from a session.
 */
public class ProducerWrapper {

    /**
     * Creates an XMLMessageProducer from a JCSMP session.
     */
    public static XMLMessageProducer createProducer(JCSMPSession session) throws Exception {
        // Get producer without streaming callback handler for synchronous operation
        return session.getMessageProducer(null);
    }
}
