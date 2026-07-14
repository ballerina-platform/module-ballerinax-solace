// Copyright (c) 2026 WSO2 LLC. (http://www.wso2.org).
//
// WSO2 LLC. licenses this file to you under the Apache License,
// Version 2.0 (the "License"); you may not use this file except
// in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

isolated function validateConfigurations(CommonConnectionConfiguration config) returns Error? {
    // Validate compression level
    int compressionLevel = config.compressionLevel;
    if compressionLevel < 0 {
        return error Error("ZLIB compression level must be at least 0 (no compression)");
    }
    if compressionLevel > 9 {
        return error Error("ZLIB compression level cannot exceed 9 (maximum compression)");
    }

    // Validate auth configurations
    AuthConfiguration? authConfig = config.auth;
    if authConfig is BasicAuthConfiguration {
        string username = authConfig.username;
        if username.length() > 32 {
            return error Error("Username cannot exceed 32 characters");
        }

        string? password = authConfig.password;
        if password is string && password.length() > 128 {
            return error Error("Password cannot exceed 128 characters");
        }
    }

    // Validate secure-socket configurations
    SecureSocket? secureSocket = config.secureSocket;
    if secureSocket is SecureSocket {
        string[]? trustedCommonNames = secureSocket.trustedCommonNames;
        if trustedCommonNames is string[] && trustedCommonNames.length() > 16 {
            return error Error("Trusted common names list cannot exceed 16 entries");
        }
    }
}

isolated function validateConsumerConfigurations(ConsumerConfiguration config) returns Error? {
    check validateConfigurations(config);

    SubscriptionConfiguration subscriptionConfig = config.subscriptionConfig;
    if subscriptionConfig is TopicConfiguration && subscriptionConfig.durability == DURABLE {
        string? endpointName = subscriptionConfig.endpointName;
        if endpointName !is string || endpointName == "" {
            return error Error("endpointName is required when durability is DURABLE");
        }
    }
}

isolated function validateMessage(Message message) returns Error? {
    int? priority = message.priority;
    if priority is int && (priority < 0 || priority > 9) {
        return error Error("priority must be between 0 and 9");
    }
}
