// Copyright (c) 2025 WSO2 LLC. (http://www.wso2.com).
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

// Solace broker connection details
const string BROKER_URL = "smf://localhost:55554";
const string MESSAGE_VPN = "default";
const string BROKER_USERNAME = "admin";
const string BROKER_PASSWORD = "admin";

// Test queue and topic names
const string TEST_QUEUE = "test-queue";
const string TEST_TOPIC = "test/topic";
const string TEST_TRANSACTED_QUEUE = "test-transacted-queue";

// Test message content
const string TEXT_MESSAGE_CONTENT = "Hello from Ballerina Solace Connector";
const string TEXT_MESSAGE_CONTENT_2 = "Second test message";
