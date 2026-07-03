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

package io.xlibb.solace.listener;

import com.solacesystems.jcsmp.BytesXMLMessage;
import com.solacesystems.jcsmp.JCSMPException;
import com.solacesystems.jcsmp.XMLMessageListener;
import io.ballerina.runtime.api.Runtime;
import io.ballerina.runtime.api.concurrent.StrandMetadata;
import io.ballerina.runtime.api.values.BError;
import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BObject;
import io.ballerina.runtime.api.values.BString;
import io.xlibb.solace.common.CommonUtils;
import io.xlibb.solace.consumer.MessageConverter;

/**
 * JCSMP asynchronous message listener that bridges broker delivery to a Ballerina service.
 * <p>
 * The broker pushes each message to {@link #onReceive(BytesXMLMessage)} on a JCSMP consumer-dispatch thread. The
 * message is converted to a Ballerina record and the service's {@code onMessage} method is invoked synchronously, which
 * gives natural back-pressure on guaranteed flows (the next message is not delivered until processing returns).
 */
final class SolaceMessageListener implements XMLMessageListener {

    private static final String ON_MESSAGE = "onMessage";
    private static final String ON_ERROR = "onError";

    private final Runtime runtime;
    private final BObject service;
    private final BObject caller;
    private final boolean hasCaller;
    private final boolean hasOnError;
    private final boolean autoAck;

    SolaceMessageListener(Runtime runtime, BObject service, BObject caller, boolean hasCaller, boolean hasOnError,
                          boolean autoAck) {
        this.runtime = runtime;
        this.service = service;
        this.caller = caller;
        this.hasCaller = hasCaller;
        this.hasOnError = hasOnError;
        this.autoAck = autoAck;
    }

    @Override
    public void onReceive(BytesXMLMessage message) {
        try {
            BMap<BString, Object> ballerinaMessage = MessageConverter.toBallerinaMessage(message);
            Object result = invokeOnMessage(ballerinaMessage);
            if (result instanceof BError bError) {
                // Processing failed: leave the message unsettled so guaranteed flows redeliver it.
                dispatchError(bError);
                return;
            }
            // In AUTO_ACK mode the flow is created with client acknowledgement, so settle on success here.
            if (autoAck) {
                message.ackMessage();
            }
        } catch (BError bError) {
            dispatchError(bError);
        } catch (Throwable t) {
            dispatchError(CommonUtils.createError("Failed to dispatch message to service",
                    t instanceof Exception e ? e : new Exception(t)));
        }
    }

    @Override
    public void onException(JCSMPException exception) {
        dispatchError(CommonUtils.createError("Solace consumer flow error", exception));
    }

    private Object invokeOnMessage(BMap<BString, Object> ballerinaMessage) {
        StrandMetadata metadata = new StrandMetadata(false, null);
        if (hasCaller) {
            return runtime.callMethod(service, ON_MESSAGE, metadata, ballerinaMessage, caller);
        }
        return runtime.callMethod(service, ON_MESSAGE, metadata, ballerinaMessage);
    }

    private void dispatchError(BError error) {
        if (!hasOnError) {
            return;
        }
        try {
            runtime.callMethod(service, ON_ERROR, new StrandMetadata(false, null), error);
        } catch (Throwable ignored) {
            // Suppress secondary failures from the error handler to avoid losing the original error.
        }
    }
}
