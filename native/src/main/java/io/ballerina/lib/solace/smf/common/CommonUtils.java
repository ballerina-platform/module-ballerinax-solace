package io.ballerina.lib.solace.smf.common;

import java.util.concurrent.CompletableFuture;
import io.ballerina.runtime.api.creators.ErrorCreator;
import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.values.BError;

/**
 * Utility class for common operations like error creation and virtual thread execution.
 */
public class CommonUtils {

    /**
     * Creates a Ballerina error from a message and Java exception.
     */
    public static BError createError(String message, Throwable cause) {
        String errorMsg = message;
        if (cause != null) {
            errorMsg = message + ": " + cause.getMessage();
        }
        return ErrorCreator.createError(StringUtils.fromString(errorMsg));
    }

    /**
     * Creates a Ballerina error from just a message.
     */
    public static BError createError(String message) {
        return ErrorCreator.createError(StringUtils.fromString(message));
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

    /**
     * Functional interface for operations that can throw checked exceptions.
     */
    @FunctionalInterface
    public interface RunnableWithException {
        void run() throws Exception;
    }
}
