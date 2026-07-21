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
import io.ballerina.runtime.api.Module;
import io.ballerina.runtime.api.types.AnnotatableType;
import io.ballerina.runtime.api.types.RemoteMethodType;
import io.ballerina.runtime.api.types.ServiceType;
import io.ballerina.runtime.api.types.Type;
import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.utils.TypeUtils;
import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BObject;
import io.ballerina.runtime.api.values.BString;

import java.util.Optional;
import java.util.stream.Stream;

/**
 * Native representation of a compiler-validated Ballerina Solace listener service object.
 */
public class Service {

    private static final String SERVICE_CONFIG_ANNOTATION_NAME = "ServiceConfig";
    private static final String ON_MESSAGE = "onMessage";
    private static final String ON_ERROR = "onError";

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

        this.hasCaller = onMessage.getParameters().length == 2;
        this.messagePayloadType = TypeUtils.getReferredType(onMessage.getParameters()[0].type);
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
