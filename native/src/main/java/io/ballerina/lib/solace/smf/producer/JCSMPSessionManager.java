package io.ballerina.lib.solace.smf.producer;

import com.solacesystems.jcsmp.JCSMPFactory;
import com.solacesystems.jcsmp.JCSMPProperties;
import com.solacesystems.jcsmp.JCSMPSession;
import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BString;

/**
 * Manager for creating JCSMP sessions and building properties from Ballerina configuration.
 */
public class JCSMPSessionManager {

    private static final String HOST = "host";
    private static final String PORT = "port";
    private static final String USERNAME = "username";
    private static final String PASSWORD = "password";
    private static final String VPN_NAME = "vpnName";
    private static final String TRANSPORT_PROTOCOL = "transportProtocol";
    private static final String CONNECTION_TIMEOUT = "connectionTimeout";
    private static final String READ_TIMEOUT = "readTimeout";

    /**
     * Builds JCSMPProperties from Ballerina configuration map.
     */
    public static JCSMPProperties buildProperties(BMap<BString, Object> config) throws Exception {
        JCSMPProperties props = new JCSMPProperties();

        // Extract and set required properties
        String host = getStringValue(config, HOST);
        int port = getIntValue(config, PORT, 55555);
        String username = getStringValue(config, USERNAME);
        String password = getStringValue(config, PASSWORD);

        // Build host string with port
        String hostString = host + ":" + port;
        props.setProperty(JCSMPProperties.HOST, hostString);

        // Set authentication
        props.setProperty(JCSMPProperties.USERNAME, username);
        props.setProperty(JCSMPProperties.PASSWORD, password);

        // Set VPN name
        String vpnName = getStringValue(config, VPN_NAME, "default");
        props.setProperty(JCSMPProperties.VPN_NAME, vpnName);

        // Set transport protocol if specified
        String transportProtocol = getStringValue(config, TRANSPORT_PROTOCOL, "PLAIN_TEXT");
        if ("PLAIN_TEXT".equals(transportProtocol)) {
            props.setProperty(JCSMPProperties.TRANSPORT_PROTOCOL_PLAIN_TEXT, null);
        }

        // Note: JCSMP connection timeouts are typically configured via the broker connection string
        // These properties may not be directly settable - we'll skip them for now

        return props;
    }

    /**
     * Creates a JCSMP session from configuration.
     */
    public static JCSMPSession createSession(BMap<BString, Object> config) throws Exception {
        JCSMPProperties props = buildProperties(config);
        return JCSMPFactory.onlyInstance().createSession(props);
    }

    /**
     * Helper to extract string value from BMap.
     */
    private static String getStringValue(BMap<BString, Object> map, String key) {
        Object value = map.get(StringUtils.fromString(key));
        if (value instanceof BString) {
            return ((BString) value).getValue();
        }
        return null;
    }

    /**
     * Helper to extract string value from BMap with default.
     */
    private static String getStringValue(BMap<BString, Object> map, String key, String defaultValue) {
        String value = getStringValue(map, key);
        return value != null ? value : defaultValue;
    }

    /**
     * Helper to extract int value from BMap.
     */
    private static int getIntValue(BMap<BString, Object> map, String key, int defaultValue) {
        Object value = map.get(StringUtils.fromString(key));
        if (value instanceof Long) {
            return ((Long) value).intValue();
        } else if (value instanceof Integer) {
            return (Integer) value;
        }
        return defaultValue;
    }

    /**
     * Helper to convert decimal (from Ballerina) to milliseconds.
     */
    private static long getDecimalAsLong(BMap<BString, Object> map, String key, long defaultValue) {
        Object value = map.get(StringUtils.fromString(key));
        if (value instanceof Double) {
            // Convert from seconds to milliseconds
            return (long) ((Double) value * 1000);
        } else if (value instanceof Long) {
            return ((Long) value * 1000);
        }
        return defaultValue;
    }
}
