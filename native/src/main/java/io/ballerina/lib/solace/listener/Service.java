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

import io.ballerina.lib.solace.ModuleUtils;
import io.ballerina.lib.solace.common.CommonUtils;
import io.ballerina.runtime.api.Module;
import io.ballerina.runtime.api.Runtime;
import io.ballerina.runtime.api.creators.TypeCreator;
import io.ballerina.runtime.api.creators.ValueCreator;
import io.ballerina.runtime.api.types.AnnotatableType;
import io.ballerina.runtime.api.types.Parameter;
import io.ballerina.runtime.api.types.RemoteMethodType;
import io.ballerina.runtime.api.types.ServiceType;
import io.ballerina.runtime.api.types.Type;
import io.ballerina.runtime.api.types.TypeTags;
import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.utils.TypeUtils;
import io.ballerina.runtime.api.values.BError;
import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BObject;
import io.ballerina.runtime.api.values.BString;

import java.util.Optional;
import java.util.stream.Stream;

/**
 * Native representation of the Ballerina Solace listener service object. Performs the service-shape
 * validation ({@code @solace:ServiceConfig} presence, {@code onMessage}/{@code onError} arity and
 * parameter types) that, absent a compiler plugin, must be done at runtime via reflection over
 * {@link RemoteMethodType}/{@link Parameter}.
 */
public class Service {

    private static final String IS_SOLACE_MESSAGE_FUNCTION = "isSolaceMessage";
    private static final String SERVICE_CONFIG_ANNOTATION_NAME = "ServiceConfig";
    private static final String ON_MESSAGE = "onMessage";
    private static final String ON_ERROR = "onError";

    private static final Type CALLER_TYPE = ValueCreator.createObjectValue(
            ModuleUtils.getModule(), "Caller").getOriginalType();
    private static final Type ERROR_TYPE = TypeCreator.createErrorType("Error", ModuleUtils.getModule());

    private final BObject consumerService;
    private final ServiceType serviceType;
    private final RemoteMethodType onMessage;
    private final Optional<RemoteMethodType> onError;
    private final boolean hasCaller;
    private final Type messagePayloadType;

    Service(BObject consumerService) {
        this.consumerService = consumerService;
        this.serviceType = (ServiceType) TypeUtils.getImpliedType(TypeUtils.getType(consumerService));
        this.onMessage = Stream.of(serviceType.getRemoteMethods())
                .filter(m -> ON_MESSAGE.equals(m.getName()))
                .findFirst().orElseThrow();
        this.onError = Stream.of(serviceType.getRemoteMethods())
                .filter(m -> ON_ERROR.equals(m.getName()))
                .findFirst();

        boolean callerFound = false;
        Type payloadType = null;
        for (Parameter parameter : onMessage.getParameters()) {
            Type referredType = TypeUtils.getReferredType(parameter.type);
            if (referredType.getTag() == TypeTags.OBJECT_TYPE_TAG) {
                callerFound = true;
            } else {
                payloadType = referredType;
            }
        }
        this.hasCaller = callerFound;
        this.messagePayloadType = payloadType;
    }

    /**
     * Validates that a service attached to the listener has the expected shape: the
     * {@code @solace:ServiceConfig} annotation, no resource methods, and one or two remote methods
     * named {@code onMessage} (required) / {@code onError} (optional) with parameter types that are
     * either a {@code solace:Message} subtype or {@code solace:Caller}.
     *
     * @throws BError if the service does not conform to the expected shape
     */
    public static void validateService(Runtime runtime, BObject consumerService) throws BError {
        ServiceType serviceType = (ServiceType) TypeUtils.getImpliedType(TypeUtils.getType(consumerService));

        if (getServiceConfigAnnotation(consumerService) == null) {
            throw CommonUtils.createError(
                    "The @solace:ServiceConfig annotation with a queue or topic subscription is required");
        }

        if (serviceType.getResourceMethods().length > 0) {
            throw CommonUtils.createError("Solace service cannot have resource methods.");
        }

        RemoteMethodType[] remoteMethods = serviceType.getRemoteMethods();
        if (remoteMethods.length < 1 || remoteMethods.length > 2) {
            throw CommonUtils.createError("Solace service must have exactly one or two remote methods.");
        }

        boolean hasOnMessage = false;
        for (RemoteMethodType remoteMethod : remoteMethods) {
            String remoteMethodName = remoteMethod.getName();
            if (ON_MESSAGE.equals(remoteMethodName)) {
                hasOnMessage = true;
                validateOnMessageMethod(runtime, remoteMethod);
            } else if (ON_ERROR.equals(remoteMethodName)) {
                validateOnErrorMethod(remoteMethod);
            } else {
                throw CommonUtils.createError(String.format("Invalid remote method name: %s.", remoteMethodName));
            }
        }

        if (!hasOnMessage) {
            throw CommonUtils.createError("The service must declare a remote 'onMessage' method");
        }
    }

    private static void validateOnMessageMethod(Runtime runtime, RemoteMethodType onMessageMethod) {
        Parameter[] parameters = onMessageMethod.getParameters();
        if (parameters.length < 1 || parameters.length > 2) {
            throw CommonUtils.createError("onMessage method can have only have either one or two parameters.");
        }

        boolean hasMessage = false;
        for (Parameter parameter : parameters) {
            Type parameterType = TypeUtils.getReferredType(parameter.type);
            if (isSolaceMessage(runtime, parameterType)) {
                hasMessage = true;
                continue;
            }
            if (TypeUtils.isSameType(CALLER_TYPE, parameterType)) {
                continue;
            }
            throw CommonUtils.createError(
                    "onMessage method parameters must be of type 'solace:Message' " +
                            "(or its subtype) or 'solace:Caller'.");
        }

        if (!hasMessage) {
            throw CommonUtils.createError("Required parameter 'solace:Message' can not be found.");
        }
    }

    private static boolean isSolaceMessage(Runtime runtime, Type paramType) {
        return (boolean) runtime.callFunction(ModuleUtils.getModule(), IS_SOLACE_MESSAGE_FUNCTION, null,
                ValueCreator.createTypedescValue(paramType));
    }

    private static void validateOnErrorMethod(RemoteMethodType onErrorMethod) {
        if (onErrorMethod.getParameters().length != 1) {
            throw CommonUtils.createError(
                    "onError method must have exactly one parameter of type 'solace:Error'.");
        }

        Parameter parameter = onErrorMethod.getParameters()[0];
        Type parameterType = TypeUtils.getReferredType(parameter.type);
        if (!TypeUtils.isSameType(ERROR_TYPE, parameterType)) {
            throw CommonUtils.createError("onError method parameter must be of type 'solace:Error'.");
        }
    }

    /**
     * Reads the {@code @solace:ServiceConfig} annotation value from the service type, or null if absent.
     */
    @SuppressWarnings("unchecked")
    static BMap<BString, Object> getServiceConfigAnnotation(BObject service) {
        Type serviceType = TypeUtils.getImpliedType(TypeUtils.getType(service));
        if (!(serviceType instanceof AnnotatableType annotatableType)) {
            return null;
        }
        Module module = ModuleUtils.getModule();
        BString annotationKey = StringUtils.fromString(module.getOrg() + "/" + module.getName() + ":"
                + module.getMajorVersion() + ":" + SERVICE_CONFIG_ANNOTATION_NAME);
        Object annotation = annotatableType.getAnnotation(annotationKey);
        if (annotation instanceof BMap) {
            return (BMap<BString, Object>) annotation;
        }
        return null;
    }

    public boolean isOnMessageMethodIsolated() {
        return this.serviceType.isIsolated() && this.onMessage.isIsolated();
    }

    public boolean isOnErrorMethodIsolated() {
        return this.onError.map(m -> this.serviceType.isIsolated() && m.isIsolated()).orElse(false);
    }

    public boolean hasCaller() {
        return hasCaller;
    }

    /**
     * Returns the declared type of the {@code onMessage} message parameter - the full narrowed
     * {@code record {|*Message; T payload;|}} type (or the base {@code Message} type if undeclared),
     * used to resolve the target type for payload data binding.
     */
    public Type getMessagePayloadType() {
        return messagePayloadType;
    }

    public BObject getConsumerService() {
        return consumerService;
    }

    public Optional<RemoteMethodType> getOnError() {
        return onError;
    }
}
