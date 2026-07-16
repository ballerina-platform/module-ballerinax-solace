/*
 * Copyright (c) 2026, WSO2 LLC. (http://www.wso2.com).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.ballerina.lib.solace.compiler;

/**
 * Diagnostics emitted by the Solace compiler plugin.
 */
enum DiagnosticCode {
    MISSING_SERVICE_CONFIG("SOLACE_101", "service must have the ''solace:ServiceConfig'' annotation"),
    RESOURCE_METHOD("SOLACE_102", "resource methods are not allowed in a ''solace:Service''"),
    INVALID_REMOTE_METHOD_SET("SOLACE_103", "service must declare ''onMessage'' and may declare only one optional " +
            "''onError'' remote method"),
    UNSUPPORTED_REMOTE_METHOD("SOLACE_104", "unsupported remote method ''{0}''; only ''onMessage'' and ''onError'' " +
            "are allowed"),
    INVALID_ON_MESSAGE_PARAMETERS("SOLACE_105", "''onMessage'' must declare exactly one ''solace:Message'' parameter " +
            "followed by an optional ''solace:Caller''"),
    INVALID_MESSAGE_PARAMETER("SOLACE_106", "parameter ''{0}'' must be ''solace:Message'' (or a supported subtype)"),
    INVALID_CALLER_PARAMETER("SOLACE_107", "the second parameter (optional) must be of type ''solace:Caller''"),
    INVALID_ON_ERROR_PARAMETER("SOLACE_108", "''onError'' must declare exactly one ''solace:Error'' parameter"),
    INVALID_RETURN_TYPE("SOLACE_109", "remote method ''{0}'' must return a value assignable to ''error?''"),
    MISSING_QUEUE_NAME("SOLACE_201", "queueName is required when the queue is DURABLE"),
    MISSING_ENDPOINT_NAME("SOLACE_202", "endpointName is required when the topic is DURABLE");

    private final String code;
    private final String message;

    DiagnosticCode(String code, String message) {
        this.code = code;
        this.message = message;
    }

    String code() {
        return code;
    }

    String message() {
        return message;
    }
}
