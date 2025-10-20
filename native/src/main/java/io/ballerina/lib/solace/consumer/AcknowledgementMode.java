/*
 * Copyright (c) 2025, WSO2 LLC. (http://www.wso2.org).
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

package io.ballerina.lib.solace.consumer;

import javax.jms.Session;

/**
 * JMS session acknowledgement modes.
 */
public enum AcknowledgementMode {
    SESSION_TRANSACTED(Session.SESSION_TRANSACTED),
    AUTO_ACKNOWLEDGE(Session.AUTO_ACKNOWLEDGE),
    CLIENT_ACKNOWLEDGE(Session.CLIENT_ACKNOWLEDGE),
    DUPS_OK_ACKNOWLEDGE(Session.DUPS_OK_ACKNOWLEDGE);

    private final int jmsMode;

    AcknowledgementMode(int jmsMode) {
        this.jmsMode = jmsMode;
    }

    /**
     * Returns the JMS session acknowledgement mode constant.
     *
     * @return JMS acknowledgement mode
     */
    public int getJmsMode() {
        return jmsMode;
    }

    /**
     * Converts Ballerina acknowledgement mode string to JMS mode.
     *
     * @param mode Ballerina acknowledgement mode
     * @return JMS acknowledgement mode constant
     */
    public static int fromString(String mode) {
        return valueOf(mode).getJmsMode();
    }
}
