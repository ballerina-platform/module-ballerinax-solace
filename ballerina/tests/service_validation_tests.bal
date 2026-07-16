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

import ballerina/test;

// ========================================
// Service-config flow-control bounds validation tests (attach()-time; same shared bounds check used
// by the pull-based MessageConsumer's subscriptionConfig, enforced natively for both paths).
// ========================================

@test:Config {groups: ["listener", "validation", "negative"]}
function testValidationServiceTransportWindowSizeTooHigh() returns error? {
    Listener solaceListener = check new (BROKER_URL, {...connectionConfig()});
    Service invalidWindowSizeService = @ServiceConfig {
        queueName: BINDING_VALIDATION_QUEUE,
        transportWindowSize: 256
    } service object {
        remote function onMessage(Message message) returns error? {
        }
    };
    error? result = solaceListener.attach(invalidWindowSizeService);
    test:assertTrue(result is error, "transportWindowSize above 255 should fail validation");
    if result is error {
        test:assertEquals(result.message(), "Failed to attach service: transportWindowSize must be between 1 and 255");
    }
    check solaceListener.gracefulStop();
}

@test:Config {groups: ["listener", "validation", "negative"]}
function testValidationServiceAckThresholdTooHigh() returns error? {
    Listener solaceListener = check new (BROKER_URL, {...connectionConfig()});
    Service invalidAckThresholdService = @ServiceConfig {
        queueName: BINDING_VALIDATION_QUEUE,
        ackThreshold: 76
    } service object {
        remote function onMessage(Message message) returns error? {
        }
    };
    error? result = solaceListener.attach(invalidAckThresholdService);
    test:assertTrue(result is error, "ackThreshold above 75 should fail validation");
    if result is error {
        test:assertEquals(result.message(), "Failed to attach service: ackThreshold must be between 1 and 75");
    }
    check solaceListener.gracefulStop();
}

@test:Config {groups: ["listener", "validation", "negative"]}
function testValidationServiceAckTimerTooHigh() returns error? {
    Listener solaceListener = check new (BROKER_URL, {...connectionConfig()});
    Service invalidAckTimerService = @ServiceConfig {
        queueName: BINDING_VALIDATION_QUEUE,
        ackTimer: 2.0
    } service object {
        remote function onMessage(Message message) returns error? {
        }
    };
    error? result = solaceListener.attach(invalidAckTimerService);
    test:assertTrue(result is error, "ackTimer above 1.5 seconds should fail validation");
    if result is error {
        test:assertEquals(result.message(), "Failed to attach service: ackTimer must be between 0.02 and 1.5 seconds");
    }
    check solaceListener.gracefulStop();
}
