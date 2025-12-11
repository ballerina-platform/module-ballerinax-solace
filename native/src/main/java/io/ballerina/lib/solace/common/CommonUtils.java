package io.ballerina.lib.solace.common;

import io.ballerina.lib.solace.ModuleUtils;
import io.ballerina.runtime.api.creators.ErrorCreator;
import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.values.BError;

import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

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
}
