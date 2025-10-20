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

package io.ballerina.lib.solace.producer;

import com.solacesystems.jms.SolConnectionFactory;
import com.solacesystems.jms.SolJmsUtility;
import io.ballerina.lib.solace.config.ConnectionConfiguration;
import io.ballerina.lib.solace.config.ConnectionUtils;
import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BObject;
import io.ballerina.runtime.api.values.BString;

import java.util.Hashtable;
import java.util.concurrent.CompletableFuture;

import javax.jms.Connection;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageProducer;
import javax.jms.Session;

/**
 * Actions class for {@link javax.jms.MessageProducer} with utility methods to invoke as inter-op functions.
 */
public final class Actions {

    private static final String NATIVE_PRODUCER = "native.producer";
    private static final String NATIVE_SESSION = "native.session";
    private static final String NATIVE_CONNECTION = "native.connection";

    private Actions() {}

    /**
     * Creates a {@link javax.jms.MessageProducer} using the broker URL and producer configurations.
     *
     * @param producer Ballerina producer object
     * @param url      Solace broker URL
     * @param config   Ballerina producer configurations
     * @return {@code null} on success, or Ballerina {@code solace:Error} on failure
     */
    public static Object init(BObject producer, BString url, BMap<BString, Object> config) {
        try {
            ProducerConfiguration producerConfig = new ProducerConfiguration(config);
            ConnectionConfiguration connConfig = producerConfig.connectionConfig();
            Hashtable<String, Object> connectionProps = ConnectionUtils.buildConnectionProperties(
                    url.getValue(), connConfig);
            SolConnectionFactory connectionFactory = SolJmsUtility.createConnectionFactory(connectionProps);

            // Configure transport mode from connection configuration
            connectionFactory.setDirectTransport(connConfig.directTransport());
            connectionFactory.setDirectOptimized(connConfig.directOptimized());

            Connection connection = connectionFactory.createConnection();
            connection.start();

            Session session = connection.createSession(producerConfig.transacted(), Session.AUTO_ACKNOWLEDGE);
            Destination destination = SolaceUtils.createDestination(session, producerConfig.destination());
            MessageProducer jmsProducer = session.createProducer(destination);

            producer.addNativeData(NATIVE_PRODUCER, jmsProducer);
            producer.addNativeData(NATIVE_SESSION, session);
            producer.addNativeData(NATIVE_CONNECTION, connection);

            return null;
        } catch (JMSException exception) {
            return SolaceUtils.createError(
                    String.format("Error occurred while initializing the Solace MessageProducer: %s",
                            exception.getMessage()), exception);
        } catch (Exception exception) {
            return SolaceUtils.createError(
                    String.format("Unexpected error occurred during producer initialization: %s",
                            exception.getMessage()), exception);
        }
    }

    /**
     * Sends a message to a destination in the Solace message broker.
     *
     * @param producer Ballerina producer object
     * @param bMessage Ballerina Solace JMS message representation
     * @return {@code null} on success, or Ballerina {@code solace:Error} on failure
     */
    public static Object send(BObject producer, BMap<BString, Object> bMessage) {
        MessageProducer nativeProducer = (MessageProducer) producer.getNativeData(NATIVE_PRODUCER);
        Session nativeSession = (Session) producer.getNativeData(NATIVE_SESSION);

        CompletableFuture<Object> future = new CompletableFuture<>();
        Thread.startVirtualThread(() -> {
            try {
                Message message = MessageConverter.toJmsMessage(nativeSession, bMessage);
                nativeProducer.send(message);
                future.complete(null);
            } catch (JMSException exception) {
                future.complete(SolaceUtils.createError(
                        String.format("Error occurred while sending message to Solace broker: %s",
                                exception.getMessage()), exception));
            } catch (Exception exception) {
                future.complete(SolaceUtils.createError(
                        String.format("Unexpected error occurred while sending message: %s",
                                exception.getMessage()), exception));
            }
        });

        try {
            return future.get();
        } catch (Exception exception) {
            return SolaceUtils.createError(
                    String.format("Error occurred while waiting for operation to complete: %s",
                            exception.getMessage()), exception);
        }
    }

    /**
     * Commits all messages done in this transaction and releases any locks currently held.
     *
     * @param producer  Ballerina producer object
     * @return          {@code null} on success, or Ballerina {@code solace:Error} if the session is not using a local
     *                  transaction or if the broker fails to commit the transaction due to some internal error
     */
    public static Object commit(BObject producer) {
        Session nativeSession = (Session) producer.getNativeData(NATIVE_SESSION);
        if (nativeSession == null) {
            return SolaceUtils.createError("Cannot commit transaction: session is not initialized");
        }
        try {
            nativeSession.commit();
            return null;
        } catch (JMSException exception) {
            return SolaceUtils.createError(
                    String.format("Error occurred while committing the transaction: %s",
                            exception.getMessage()), exception);
        }
    }

    /**
     * Rolls back any messages done in this transaction and releases any locks currently held.
     *
     * @param producer Ballerina producer object
     * @return {@code null} on success, or Ballerina {@code solace:Error} if the session is not using
     * a local transaction or if the broker fails to roll back the transaction due to some internal error
     */
    public static Object rollback(BObject producer) {
        Session nativeSession = (Session) producer.getNativeData(NATIVE_SESSION);
        if (nativeSession == null) {
            return SolaceUtils.createError("Cannot rollback transaction: session is not initialized");
        }
        try {
            nativeSession.rollback();
            return null;
        } catch (JMSException exception) {
            return SolaceUtils.createError(
                    String.format("Error occurred while rolling back the transaction: %s",
                            exception.getMessage()), exception);
        }
    }

    /**
     * Closes the message producer and the underlying connection.
     *
     * @param producer Ballerina producer object
     * @return {@code null} on success, or Ballerina {@code solace:Error} on failure
     */
    public static Object close(BObject producer) {
        try {
            MessageProducer nativeProducer = (MessageProducer) producer.getNativeData(NATIVE_PRODUCER);
            Session nativeSession = (Session) producer.getNativeData(NATIVE_SESSION);
            Connection nativeConnection = (Connection) producer.getNativeData(NATIVE_CONNECTION);

            if (nativeProducer != null) {
                nativeProducer.close();
            }
            if (nativeSession != null) {
                nativeSession.close();
            }
            if (nativeConnection != null) {
                nativeConnection.close();
            }

            return null;
        } catch (JMSException exception) {
            return SolaceUtils.createError(
                    String.format("Error occurred while closing the message producer: %s",
                            exception.getMessage()), exception);
        }
    }
}
