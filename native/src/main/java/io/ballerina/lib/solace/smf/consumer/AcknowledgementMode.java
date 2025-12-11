package io.ballerina.lib.solace.smf.consumer;

import com.solacesystems.jcsmp.JCSMPProperties;

public enum AcknowledgementMode {
    AUTO_ACK(JCSMPProperties.SUPPORTED_MESSAGE_ACK_CLIENT),
    CLIENT_ACK(JCSMPProperties.SUPPORTED_MESSAGE_ACK_CLIENT);

    private final String jcsmpMode;

    AcknowledgementMode(String jcsmpMode) {
        this.jcsmpMode = jcsmpMode;

    }

    public String getJcsmpMode() {
        return jcsmpMode;
    }
}
