/*
 * Copyright (c) 2026, WSO2 LLC. (http://www.wso2.org).
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package io.ballerina.lib.solace.consumer;

import com.solacesystems.jcsmp.CapabilityType;
import com.solacesystems.jcsmp.ConsumerFlowProperties;
import com.solacesystems.jcsmp.JCSMPSession;
import org.testng.annotations.Test;

import java.lang.reflect.Proxy;

import static org.testng.Assert.assertEquals;

public class ConsumerUtilsTest {

    @Test
    public void testActiveFlowIndicationEnabledWhenSupported() {
        ConsumerFlowProperties properties = new ConsumerFlowProperties();
        ConsumerUtils.configureActiveFlowIndication(sessionWithCapability(true), properties);
        assertEquals(properties.isActiveFlowIndication(), true);
    }

    @Test
    public void testActiveFlowIndicationDisabledWhenUnsupported() {
        ConsumerFlowProperties properties = new ConsumerFlowProperties();
        ConsumerUtils.configureActiveFlowIndication(sessionWithCapability(false), properties);
        assertEquals(properties.isActiveFlowIndication(), false);
    }

    private static JCSMPSession sessionWithCapability(boolean supported) {
        return (JCSMPSession) Proxy.newProxyInstance(
                JCSMPSession.class.getClassLoader(),
                new Class<?>[]{JCSMPSession.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("isCapable") && args[0] == CapabilityType.ACTIVE_FLOW_INDICATION) {
                        return supported;
                    }
                    throw new UnsupportedOperationException(method.getName());
                });
    }
}
