package io.ballerina.lib.solace.common;

import com.solacesystems.jcsmp.JCSMPFactory;
import com.solacesystems.jcsmp.SDTException;
import com.solacesystems.jcsmp.SDTMap;
import io.ballerina.runtime.api.creators.ValueCreator;
import io.ballerina.runtime.api.types.MapType;
import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.values.BArray;
import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BString;

/**
 * Utility for bidirectional conversion between JCSMP SDTMap and Ballerina map.
 */
public class PropertyConverter {

    /**
     * Converts a JCSMP SDTMap to a Ballerina map.
     *
     * @param sdtMap        the JCSMP SDTMap
     * @param ballerinaType the Ballerina MapType for type safety (null for untyped maps)
     * @return Ballerina map with converted values
     * @throws SDTException if SDT conversion fails
     */
    public static BMap<BString, Object> sdtMapToBallerina(SDTMap sdtMap, MapType ballerinaType)
            throws SDTException {
        // Create typed map if MapType provided, otherwise create untyped map
        BMap<BString, Object> messageProperties = (ballerinaType != null)
                ? ValueCreator.createMapValue(ballerinaType)
                : ValueCreator.createMapValue();

        for (String key : sdtMap.keySet()) {
            Object value = sdtMap.get(key);

            if (value == null) {
                continue;
            }

            // Convert SDT value to Ballerina value
            Object ballerinaValue = convertSDTValueToBallerina(value);
            if (ballerinaValue != null) {
                messageProperties.put(StringUtils.fromString(key), ballerinaValue);
            }
        }

        return messageProperties;
    }

    /**
     * Converts a Ballerina map to a JCSMP SDTMap.
     *
     * @param propsMap the Ballerina properties map
     * @return JCSMP SDTMap, or null if input is null/empty
     * @throws SDTException if SDT conversion fails
     */
    public static SDTMap ballerinaToSDTMap(BMap<BString, Object> propsMap) throws SDTException {
        if (propsMap == null || propsMap.isEmpty()) {
            return null;
        }

        SDTMap sdtMap = JCSMPFactory.onlyInstance().createMap();

        for (BString key : propsMap.getKeys()) {
            Object value = propsMap.get(key);
            String keyStr = key.getValue();

            if (value == null) {
                continue;
            }

            // Convert Ballerina value to SDT value
            convertBallerinaValueToSDT(sdtMap, keyStr, value);
        }

        return sdtMap;
    }

    /**
     * Converts an SDT value to a Ballerina-compatible value.
     *
     * @param value the SDT value
     * @return Ballerina-compatible value, or null if conversion not supported
     * @throws SDTException if nested SDTMap conversion fails
     */
    private static Object convertSDTValueToBallerina(Object value) throws SDTException {
        return switch (value) {
            case null -> null;
            case String s -> StringUtils.fromString(s);
            case Boolean b -> b;
            case Integer i -> i.longValue();
            case Long l -> l;
            case Float v -> v.doubleValue();
            case Double v -> v;
            case Byte b -> b.longValue();
            case Short i -> i.longValue();
            case byte[] bytes -> ValueCreator.createArrayValue(bytes);
            case SDTMap nestedMap -> sdtMapToBallerina(nestedMap, null);
            default -> StringUtils.fromString(value.toString());
        };

    }

    /**
     * Converts a Ballerina value to an SDT value and puts it in the SDTMap.
     *
     * @param sdtMap the target SDTMap
     * @param key    the key
     * @param value  the Ballerina value
     * @throws SDTException if SDT conversion fails
     */
    private static void convertBallerinaValueToSDT(SDTMap sdtMap, String key, Object value)
            throws SDTException {
        switch (value) {
            case null -> {
            }
            case BString bString -> sdtMap.putString(key, bString.getValue());
            case Boolean b -> sdtMap.putBoolean(key, b);
            case Long l -> sdtMap.putLong(key, l);
            case Integer i -> sdtMap.putInteger(key, i);
            case Double v -> sdtMap.putDouble(key, v);
            case Float v -> sdtMap.putFloat(key, v);
            case BArray bArray -> sdtMap.putBytes(key, bArray.getBytes());
            default -> sdtMap.putString(key, value.toString());
        }
    }

}
