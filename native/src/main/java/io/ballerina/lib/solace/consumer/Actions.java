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

package io.ballerina.lib.solace.consumer;

import com.solacesystems.jms.SolConnectionFactory;
import com.solacesystems.jms.SolJmsUtility;
import io.ballerina.lib.solace.BallerinaSolaceDatabindingException;
import io.ballerina.lib.solace.CommonUtils;
import io.ballerina.lib.solace.config.ConnectionConfiguration;
import io.ballerina.runtime.api.values.BDecimal;
import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BObject;
import io.ballerina.runtime.api.values.BString;
import io.ballerina.runtime.api.values.BTypedesc;

import java.math.BigDecimal;
import java.util.Hashtable;
import java.util.concurrent.CompletableFuture;

import javax.jms.Connection;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.Session;

/**
 * Actions class for {@link javax.jms.MessageConsumer} with utility methods to invoke as inter-op functions.
 */
public final class Actions {

    private static final String NATIVE_CONSUMER = "native.consumer";
    private static final String NATIVE_SESSION = "native.session";
    private static final String NATIVE_CONNECTION = "native.connection";

    private Actions() {
    }

    /**
     * Creates a {@link javax.jms.MessageConsumer} using the broker URL and consumer configurations.
     *
     * @param consumer Ballerina consumer object
     * @param url      Solace broker URL
     * @param config   Ballerina consumer configurations
     * @return {@code null} on success, or Ballerina {@code solace:Error} on failure
     */
    public static Object init(BObject consumer, BString url, BMap<BString, Object> config) {
        try {
            ConsumerConfiguration consumerConfig = new ConsumerConfiguration(config);
            ConnectionConfiguration connConfig = consumerConfig.connectionConfig();
            SubscriptionConfig subscriptionConfig = consumerConfig.subscriptionConfig();

            Hashtable<String, Object> connectionProps = CommonUtils.buildConnectionProperties(
                    url.getValue(), connConfig);
            SolConnectionFactory connectionFactory = SolJmsUtility.createConnectionFactory(connectionProps);

            // Configure transport mode from connection configuration
            connectionFactory.setDirectTransport(connConfig.directTransport());
            connectionFactory.setDirectOptimized(connConfig.directOptimized());

            Connection connection = connectionFactory.createConnection();
            connection.start();

            // Create session with acknowledgement mode from subscription config
            int ackMode = subscriptionConfig.sessionAckMode().getJmsMode();
            boolean transacted = (ackMode == Session.SESSION_TRANSACTED);
            Session session = connection.createSession(transacted, ackMode);

            // Create consumer based on subscription config
            MessageConsumer jmsConsumer = ConsumerUtils.createConsumer(session, subscriptionConfig);

            consumer.addNativeData(NATIVE_CONSUMER, jmsConsumer);
            consumer.addNativeData(NATIVE_SESSION, session);
            consumer.addNativeData(NATIVE_CONNECTION, connection);

            return null;
        } catch (JMSException exception) {
            return CommonUtils.createError(
                    String.format("Error occurred while initializing the Solace MessageConsumer: %s",
                            exception.getMessage()), exception);
        } catch (Exception exception) {
            return CommonUtils.createError(
                    String.format("Unexpected error occurred during consumer initialization: %s",
                            exception.getMessage()), exception);
        }
    }

    /**
     * Receives the next message from the Solace broker within the specified timeout.
     *
     * @param consumer  Ballerina consumer object
     * @param timeout   Timeout in seconds
     * @param bTypedesc Expected message type
     * @return Ballerina message, {@code null} if no message available, or {@code solace:Error} on failure
     */
    public static Object receive(BObject consumer, BDecimal timeout, BTypedesc bTypedesc) {
        MessageConsumer nativeConsumer = (MessageConsumer) consumer.getNativeData(NATIVE_CONSUMER);

        CompletableFuture<Object> future = new CompletableFuture<>();
        Thread.startVirtualThread(() -> {
            try {
                BigDecimal timeoutDecimal = timeout.decimalValue();
                long timeoutMillis = timeoutDecimal.multiply(BigDecimal.valueOf(1000)).longValue();
                Message message = nativeConsumer.receive(timeoutMillis);

                if (message == null) {
                    future.complete(null);
                } else {
                    BMap<BString, Object> ballerinaMessage = MessageConverter.toBallerinaMessage(message, bTypedesc);
                    future.complete(ballerinaMessage);
                }
            } catch (JMSException exception) {
                future.complete(CommonUtils.createError(
                        String.format("Error occurred while receiving message from Solace broker: %s",
                                exception.getMessage()), exception));
            } catch (BallerinaSolaceDatabindingException exception) {
                future.complete(CommonUtils.createError(exception.getMessage(), exception));
            } catch (Exception exception) {
                future.complete(CommonUtils.createError(
                        String.format("Unexpected error occurred while receiving message: %s",
                                exception.getMessage()), exception));
            }
        });

        try {
            return future.get();
        } catch (Exception exception) {
            return CommonUtils.createError(
                    String.format("Error occurred while waiting for operation to complete: %s",
                            exception.getMessage()), exception);
        }
    }

    /**
     * Receives the next message from the Solace broker if one is immediately available.
     *
     * @param consumer  Ballerina consumer object
     * @param bTypedesc Expected message type
     * @return Ballerina message, {@code null} if no message available, or {@code solace:Error} on failure
     */
    public static Object receiveNoWait(BObject consumer, BTypedesc bTypedesc) {
        MessageConsumer nativeConsumer = (MessageConsumer) consumer.getNativeData(NATIVE_CONSUMER);

        CompletableFuture<Object> future = new CompletableFuture<>();
        Thread.startVirtualThread(() -> {
            try {
                Message message = nativeConsumer.receiveNoWait();

                if (message == null) {
                    future.complete(null);
                } else {
                    BMap<BString, Object> ballerinaMessage = MessageConverter.toBallerinaMessage(message, bTypedesc);
                    future.complete(ballerinaMessage);
                }
            } catch (JMSException exception) {
                future.complete(CommonUtils.createError(
                        String.format("Error occurred while receiving message from Solace broker: %s",
                                exception.getMessage()), exception));
            } catch (BallerinaSolaceDatabindingException exception) {
                future.complete(CommonUtils.createError(exception.getMessage(), exception));
            } catch (Exception exception) {
                future.complete(CommonUtils.createError(
                        String.format("Unexpected error occurred while receiving message: %s",
                                exception.getMessage()), exception));
            }
        });

        try {
            return future.get();
        } catch (Exception exception) {
            return CommonUtils.createError(
                    String.format("Error occurred while waiting for operation to complete: %s",
                            exception.getMessage()), exception);
        }
    }

    /**
     * Acknowledges the specified message.
     *
     * @param message Ballerina message to acknowledge
     * @return {@code null} on success, or Ballerina {@code solace:Error} on failure
     */
    public static Object acknowledge(BMap<BString, Object> message) {
        try {
            // Retrieve the native JMS message stored in the Ballerina message
            Message nativeMessage = (Message) message.getNativeData(MessageConverter.NATIVE_MESSAGE);
            if (nativeMessage == null) {
                return CommonUtils.createError("Cannot acknowledge message: native message not found");
            }

            // Acknowledge the message (acknowledges all messages in the session for CLIENT_ACKNOWLEDGE mode)
            nativeMessage.acknowledge();

            return null;
        } catch (JMSException exception) {
            return CommonUtils.createError(
                    String.format("Error occurred while acknowledging the message: %s",
                            exception.getMessage()), exception);
        }
    }

    /**
     * Commits all messages received in this transaction and releases any locks currently held.
     *
     * @param consumer Ballerina consumer object
     * @return {@code null} on success, or Ballerina {@code solace:Error} on failure
     */
    public static Object commit(BObject consumer) {
        Session nativeSession = (Session) consumer.getNativeData(NATIVE_SESSION);
        if (nativeSession == null) {
            return CommonUtils.createError("Cannot commit transaction: session is not initialized");
        }
        try {
            nativeSession.commit();
            return null;
        } catch (JMSException exception) {
            return CommonUtils.createError(
                    String.format("Error occurred while committing the transaction: %s",
                            exception.getMessage()), exception);
        }
    }

    /**
     * Rolls back any messages received in this transaction and releases any locks currently held.
     *
     * @param consumer Ballerina consumer object
     * @return {@code null} on success, or Ballerina {@code solace:Error} on failure
     */
    public static Object rollback(BObject consumer) {
        Session nativeSession = (Session) consumer.getNativeData(NATIVE_SESSION);
        if (nativeSession == null) {
            return CommonUtils.createError("Cannot rollback transaction: session is not initialized");
        }
        try {
            nativeSession.rollback();
            return null;
        } catch (JMSException exception) {
            return CommonUtils.createError(
                    String.format("Error occurred while rolling back the transaction: %s",
                            exception.getMessage()), exception);
        }
    }

    /**
     * Closes the message consumer and the underlying connection.
     *
     * @param consumer Ballerina consumer object
     * @return {@code null} on success, or Ballerina {@code solace:Error} on failure
     */
    public static Object close(BObject consumer) {
        try {
            MessageConsumer nativeConsumer = (MessageConsumer) consumer.getNativeData(NATIVE_CONSUMER);
            Session nativeSession = (Session) consumer.getNativeData(NATIVE_SESSION);
            Connection nativeConnection = (Connection) consumer.getNativeData(NATIVE_CONNECTION);

            if (nativeConsumer != null) {
                nativeConsumer.close();
            }
            if (nativeSession != null) {
                nativeSession.close();
            }
            if (nativeConnection != null) {
                nativeConnection.close();
            }

            return null;
        } catch (JMSException exception) {
            return CommonUtils.createError(
                    String.format("Error occurred while closing the message consumer: %s",
                            exception.getMessage()), exception);
        }
    }
}
