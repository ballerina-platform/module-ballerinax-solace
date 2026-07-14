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

import com.solacesystems.jcsmp.BytesXMLMessage;
import com.solacesystems.jcsmp.JCSMPException;
import com.solacesystems.jcsmp.XMLMessageListener;
import io.ballerina.lib.solace.common.BallerinaSolaceDatabindingException;
import io.ballerina.lib.solace.common.CommonUtils;
import io.ballerina.lib.solace.consumer.MessageConverter;
import io.ballerina.runtime.api.Runtime;
import io.ballerina.runtime.api.concurrent.StrandMetadata;
import io.ballerina.runtime.api.creators.ValueCreator;
import io.ballerina.runtime.api.values.BError;
import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BObject;
import io.ballerina.runtime.api.values.BString;

import java.io.PrintStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * JCSMP asynchronous message listener that bridges broker delivery to a Ballerina service.
 * <p>
 * The broker pushes each message to {@link #onReceive(BytesXMLMessage)} on a JCSMP consumer-dispatch thread. To avoid
 * blocking that thread - which JCSMP also uses to process control responses such as transacted commits and to drive
 * redelivery - the message is converted to a Ballerina record on the delivery thread (so its payload is copied before
 * the buffer can be reused) and the service invocation plus any settlement (ack / nack / commit / rollback) are handed
 * off to a dedicated single-threaded executor. The single thread preserves per-flow message ordering while keeping the
 * delivery thread free.
 */
final class SolaceMessageListener implements XMLMessageListener {

    private static final String ON_MESSAGE = "onMessage";
    private static final String ON_ERROR = "onError";
    private static final PrintStream ERR_OUT = System.err;

    private final Runtime runtime;
    private final Service nativeService;
    private final BObject caller;
    private final boolean autoAck;
    private final ExecutorService dispatcher;

    SolaceMessageListener(Runtime runtime, Service nativeService, BObject caller, boolean autoAck) {
        this.runtime = runtime;
        this.nativeService = nativeService;
        this.caller = caller;
        this.autoAck = autoAck;
        this.dispatcher = Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "solace-listener-dispatch");
            thread.setDaemon(true);
            return thread;
        });
    }

    @Override
    public void onReceive(BytesXMLMessage message) {
        // Convert on the JCSMP delivery thread (copies the payload, safe for direct messages), then hand off so the
        // delivery thread is never blocked by the service call or a blocking settlement.
        BMap<BString, Object> ballerinaMessage;
        try {
            ballerinaMessage = MessageConverter.toBallerinaMessage(message,
                    ValueCreator.createTypedescValue(nativeService.getMessagePayloadType()));
        } catch (BallerinaSolaceDatabindingException e) {
            submit(() -> dispatchError(CommonUtils.createError(e.getMessage())));
            return;
        } catch (Throwable t) {
            submit(() -> dispatchError(CommonUtils.createError("Failed to convert message",
                    t instanceof Exception e ? e : new Exception(t))));
            return;
        }
        submit(() -> deliver(message, ballerinaMessage));
    }

    private void deliver(BytesXMLMessage message, BMap<BString, Object> ballerinaMessage) {
        try {
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
        submit(() -> dispatchError(CommonUtils.createError("Solace consumer flow error", exception)));
    }

    private Object invokeOnMessage(BMap<BString, Object> ballerinaMessage) {
        StrandMetadata metadata = new StrandMetadata(nativeService.isOnMessageMethodIsolated(), null);
        if (nativeService.hasCaller()) {
            return runtime.callMethod(nativeService.getConsumerService(), ON_MESSAGE, metadata, ballerinaMessage,
                    caller);
        }
        return runtime.callMethod(nativeService.getConsumerService(), ON_MESSAGE, metadata, ballerinaMessage);
    }

    private void dispatchError(BError error) {
        if (nativeService.getOnError().isEmpty()) {
            ERR_OUT.println("Unexpected error occurred while message processing: " + error.getMessage());
            error.printStackTrace();
            return;
        }
        try {
            StrandMetadata metadata = new StrandMetadata(nativeService.isOnErrorMethodIsolated(), null);
            runtime.callMethod(nativeService.getConsumerService(), ON_ERROR, metadata, error);
        } catch (Throwable t) {
            // Surface secondary failures from the error handler instead of dropping them silently;
            // do not let them propagate and take down the dispatch thread.
            ERR_OUT.println("Unexpected error occurred while invoking the 'onError' method: "
                    + (t.getMessage() != null ? t.getMessage() : t));
        }
    }

    private void submit(Runnable task) {
        try {
            dispatcher.execute(task);
        } catch (RejectedExecutionException ignored) {
            // The listener is stopping; drop late deliveries (unsettled guaranteed messages are redelivered).
        }
    }

    /**
     * Stops the dispatch executor. Called when the service is detached or the listener is stopped.
     */
    void shutdown() {
        dispatcher.shutdown();
        try {
            if (!dispatcher.awaitTermination(5, TimeUnit.SECONDS)) {
                dispatcher.shutdownNow();
            }
        } catch (InterruptedException e) {
            dispatcher.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
