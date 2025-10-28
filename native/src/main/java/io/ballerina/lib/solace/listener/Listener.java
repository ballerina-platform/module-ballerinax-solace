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

package io.ballerina.lib.solace.listener;

import com.solacesystems.jms.SolConnectionFactory;
import com.solacesystems.jms.SolJmsUtility;
import io.ballerina.lib.solace.CommonUtils;
import io.ballerina.lib.solace.config.ConnectionConfiguration;
import io.ballerina.runtime.api.Environment;
import io.ballerina.runtime.api.values.BError;
import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BObject;
import io.ballerina.runtime.api.values.BString;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.jms.Connection;
import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.Session;

/**
 * Native class for the Ballerina Solace Listener.
 *
 */
public class Listener {
    private static final String NATIVE_CONNECTION = "native.connection";
    private static final String NATIVE_SERVICE_LIST = "native.service.list";
    private static final String NATIVE_SERVICE = "native.service";
    private static final String NATIVE_RECEIVER = "native.receiver";
    private static final String LISTENER_STARTED = "listener.started";
    private static final String AUTO_ACKNOWLEDGE_MODE = "AUTO_ACKNOWLEDGE";
    private static final String CLIENT_ACKNOWLEDGE_MODE = "CLIENT_ACKNOWLEDGE";
    private static final String SESSION_TRANSACTED_MODE = "SESSION_TRANSACTED";

    private Listener() {}

    public static Object init(BObject bListener, BString url, BMap<BString, Object> config) {
        try {
            ConnectionConfiguration connConfig = new ConnectionConfiguration(config);
            Hashtable<String, Object> connectionProps = CommonUtils.buildConnectionProperties(
                    url.getValue(), connConfig);
            SolConnectionFactory connectionFactory = SolJmsUtility.createConnectionFactory(connectionProps);
            connectionFactory.setDirectTransport(connConfig.directTransport());
            connectionFactory.setDirectOptimized(connConfig.directOptimized());
            Connection connection = connectionFactory.createConnection();
            bListener.addNativeData(NATIVE_CONNECTION, connection);
            bListener.addNativeData(NATIVE_SERVICE_LIST, new ArrayList<BObject>());
        } catch (Exception e) {
            return CommonUtils.createError("Failed to initialize listener", e);
        }
        return null;
    }

    public static Object attach(Environment env, BObject bListener, BObject bService, Object name) {
        Connection connection = (Connection) bListener.getNativeData(NATIVE_CONNECTION);
        Object started = bListener.getNativeData(LISTENER_STARTED);
        try {
            Service.validateService(env.getRuntime(), bService);
            Service nativeService = new Service(bService);
            ServiceConfig svcConfig = nativeService.getServiceConfig();
            int sessionAckMode = getSessionAckMode(svcConfig.ackMode());
            boolean transacted = Session.SESSION_TRANSACTED == sessionAckMode;
            Session session = connection.createSession(transacted, sessionAckMode);
            MessageConsumer consumer = ListenerUtils.createConsumer(session, svcConfig);
            MessageDispatcher messageDispatcher = new MessageDispatcher(env.getRuntime(), nativeService, session);
            MessageReceiver receiver = new MessageReceiver(
                    session, consumer, messageDispatcher, svcConfig.pollingInterval(), svcConfig.receiveTimeout());
            bService.addNativeData(NATIVE_SERVICE, nativeService);
            bService.addNativeData(NATIVE_RECEIVER, receiver);
            List<BObject> serviceList = (List<BObject>) bListener.getNativeData(NATIVE_SERVICE_LIST);
            serviceList.add(bService);
            if (Objects.nonNull(started)) {
                AtomicBoolean listenerStarted = (AtomicBoolean) started;
                if (listenerStarted.get()) {
                    receiver.consume();
                }
            }
        } catch (BError | JMSException e) {
            String errorMsg = Objects.isNull(e.getMessage()) ? "Unknown error" : e.getMessage();
            return CommonUtils.createError(String.format("Failed to attach service to listener: %s", errorMsg), e);
        }
        return null;
    }

    private static int getSessionAckMode(String ackMode) {
        return switch (ackMode) {
            case SESSION_TRANSACTED_MODE -> Session.SESSION_TRANSACTED;
            case AUTO_ACKNOWLEDGE_MODE -> Session.AUTO_ACKNOWLEDGE;
            case CLIENT_ACKNOWLEDGE_MODE -> Session.CLIENT_ACKNOWLEDGE;
            case null, default -> Session.DUPS_OK_ACKNOWLEDGE;
        };
    }

    public static Object detach(BObject bService) {
        Object receiver = bService.getNativeData(NATIVE_RECEIVER);
        try {
            if (Objects.isNull(receiver)) {
                return CommonUtils.createError("Could not find the native Solace message receiver");
            }
            ((MessageReceiver) receiver).stop();
        } catch (Exception e) {
            String errorMsg = Objects.isNull(e.getMessage()) ? "Unknown error" : e.getMessage();
            return CommonUtils.createError(
                    String.format("Failed to detach a service from the listener: %s", errorMsg), e);
        }
        return null;
    }

    public static Object start(BObject bListener) {
        Connection connection = (Connection) bListener.getNativeData(NATIVE_CONNECTION);
        List<BObject> bServices = (List<BObject>) bListener.getNativeData(NATIVE_SERVICE_LIST);
        try {
            connection.start();
            for (BObject bService: bServices) {
                MessageReceiver receiver = (MessageReceiver) bService.getNativeData(NATIVE_RECEIVER);
                receiver.consume();
            }
            AtomicBoolean listenerStarted = new AtomicBoolean(true);
            bListener.addNativeData(LISTENER_STARTED, listenerStarted);
        } catch (JMSException e) {
            String errorMsg = Objects.isNull(e.getMessage()) ? "Unknown error" : e.getMessage();
            return CommonUtils.createError(
                    String.format("Error occurred while starting the Ballerina Solace listener: %s", errorMsg), e);
        }
        return null;
    }

    public static Object gracefulStop(BObject bListener) {
        Connection nativeConnection = (Connection) bListener.getNativeData(NATIVE_CONNECTION);
        List<BObject> bServices = (List<BObject>) bListener.getNativeData(NATIVE_SERVICE_LIST);
        try {
            for (BObject bService: bServices) {
                MessageReceiver receiver = (MessageReceiver) bService.getNativeData(NATIVE_RECEIVER);
                receiver.stop();
            }
            nativeConnection.stop();
            nativeConnection.close();
        } catch (Exception e) {
            String errorMsg = Objects.isNull(e.getMessage()) ? "Unknown error" : e.getMessage();
            return CommonUtils.createError(
                    String.format(
                            "Error occurred while gracefully stopping the Ballerina Solace listener: %s", errorMsg), e);
        }
        return null;
    }

    public static Object immediateStop(BObject bListener) {
        Connection nativeConnection = (Connection) bListener.getNativeData(NATIVE_CONNECTION);
        List<BObject> bServices = (List<BObject>) bListener.getNativeData(NATIVE_SERVICE_LIST);
        try {
            for (BObject bService: bServices) {
                MessageReceiver receiver = (MessageReceiver) bService.getNativeData(NATIVE_RECEIVER);
                receiver.stop();
            }
            nativeConnection.stop();
            nativeConnection.close();
        } catch (Exception e) {
            String errorMsg = Objects.isNull(e.getMessage()) ? "Unknown error" : e.getMessage();
            return CommonUtils.createError(
                    String.format("Error occurred while gracefully stopping the Ballerina Solace listener: %s",
                            errorMsg), e);
        }
        return null;
    }
}
