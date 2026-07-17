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

package io.ballerina.lib.solace.common;

import io.ballerina.lib.solace.ModuleUtils;
import io.ballerina.runtime.api.creators.ErrorCreator;
import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.values.BArray;
import io.ballerina.runtime.api.values.BError;
import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BString;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import static io.ballerina.lib.solace.common.MessageFieldConstants.PAYLOAD_KEY;

/**
 * Utility class for common operations like error creation and virtual thread execution.
 */
public class CommonUtils {

    private static final String SOLACE_ERROR = "Error";

    /**
     * Creates a Ballerina error from a message and Java exception.
     */
    public static BError createError(String message, Throwable cause) {
        String errorMsg = message;
        if (cause != null) {
            errorMsg = message + ": " + cause.getMessage();
        }
        return ErrorCreator.createError(ModuleUtils.getModule(), SOLACE_ERROR, StringUtils.fromString(errorMsg), null,
                null);
    }

    /**
     * Creates a Ballerina error from just a message.
     */
    public static BError createError(String message) {
        return ErrorCreator.createError(ModuleUtils.getModule(), SOLACE_ERROR,
                StringUtils.fromString(message), null, null);
    }

    /**
     * Executes a blocking operation on a virtual thread and waits for completion.
     */
    public static Object executeBlocking(RunnableWithException task) throws Exception {
        CompletableFuture<Object> future = new CompletableFuture<>();

        Thread.startVirtualThread(() -> {
            try {
                task.run();
                future.complete(null);
            } catch (Exception e) {
                future.complete(createError("Error during blocking operation", e));
            }
        });

        return future.get();
    }

    public static Object executeBlocking(CompletableWithException task) throws Exception {
        CompletableFuture<Object> future = new CompletableFuture<>();
        Thread.startVirtualThread(() -> {
            try {
                Object result = task.run();
                future.complete(result);
            } catch (Exception e) {
                future.complete(createError("Error during blocking operation", e));
            }
        });
        return future.get();
    }

    /**
     * Runs a best-effort cleanup action, silently discarding any exception. Use when a secondary failure during
     * cleanup should not surface to the caller (e.g. releasing resources already created before an init() failure).
     */
    public static void closeQuietly(RunnableWithException action) {
        try {
            action.run();
        } catch (Exception e) {
            // Best-effort cleanup - ignore.
        }
    }

    /**
     * Runs a cleanup action and returns any exception instead of throwing it, so callers can attempt several
     * independent cleanup steps without one failure blocking the rest.
     *
     * @return the caught exception, or null if the action succeeded
     */
    public static Exception attemptClose(RunnableWithException action) {
        try {
            action.run();
            return null;
        } catch (Exception e) {
            return e;
        }
    }

    /**
     * Computes the best-effort byte size of a Ballerina Solace message's payload, for observability metrics.
     * <p>
     * A message {@code payload} is {@code anydata}, which the connector serializes to one of the wire shapes
     * {@code byte[] | string | map<Value>} (see the producer's {@code convertPayload}). Only the two raw
     * byte-oriented shapes are measured exactly - a {@code byte[]} by its length and a {@code string} by its
     * UTF-8 byte length. A structured payload ({@code map<Value>}/record, or a data-bound consumer type) has no
     * well-defined serialized size at this layer, so it is reported as 0 rather than estimated.
     *
     * @param message the Ballerina message record
     * @return the payload size in bytes, or 0 if it cannot be determined
     */
    @SuppressWarnings("unchecked")
    public static int getPayloadSize(BMap<BString, Object> message) {
        if (message == null) {
            return 0;
        }
        Object payload = message.get(PAYLOAD_KEY);
        if (payload instanceof BArray arr) {
            // byte[] payload: the wire size is exactly the array length.
            return arr.size();
        }
        if (payload instanceof BString str) {
            // string payload: sized as its UTF-8 encoding, matching how it is written to the broker.
            return str.getValue().getBytes(StandardCharsets.UTF_8).length;
        }
        // Structured payload (map<Value>/record or a data-bound type): no exact wire size here, so report 0.
        return 0;
    }

    /**
     * Converts an array of Objects to an array of Strings.
     *
     * @param objectArray array of Objects
     * @return array of Strings
     */
    public static String[] convertToStringArray(Object[] objectArray) {
        if (Objects.isNull(objectArray)) {
            return new String[]{ };
        }
        return Arrays.stream(objectArray)
                .filter(Objects::nonNull)
                .map(Object::toString)
                .toArray(String[]::new);
    }

    /**
     * Functional interface for operations that can throw checked exceptions.
     */
    @FunctionalInterface
    public interface RunnableWithException {

        void run() throws Exception;
    }

    @FunctionalInterface
    public interface CompletableWithException {

        Object run() throws Exception;
    }
}
