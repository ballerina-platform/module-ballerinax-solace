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

# Checks whether a given type is a subtype of `solace:Message`.
#
# + typeDesc - The type needed to be checked as a subtype of `solace:Message`
# + return - True if the type is a subtype of `solace:Message`, else false
isolated function isSolaceMessage(typedesc<anydata> typeDesc) returns boolean {
    if typeDesc is typedesc<Message> {
        return true;
    }
    return false;
}
