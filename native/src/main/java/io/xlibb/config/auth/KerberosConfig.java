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

package io.xlibb.config.auth;

import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BString;

/**
 * Kerberos (GSS-KRB) authentication configuration.
 *
 * @param serviceName Kerberos service name used during authentication
 * @param jaasLoginContext JAAS login context name to use for authentication
 * @param mutualAuthentication true to enable Kerberos mutual authentication
 * @param jaasConfigFileReloadEnabled true to enable automatic reload of JAAS configuration file
 */
public record KerberosConfig(
        String serviceName,
        String jaasLoginContext,
        boolean mutualAuthentication,
        boolean jaasConfigFileReloadEnabled) implements AuthConfig {
    private static final BString SERVICE_NAME_KEY = StringUtils.fromString("serviceName");
    private static final BString JAAS_LOGIN_CONTEXT_KEY = StringUtils.fromString("jaasLoginContext");
    private static final BString MUTUAL_AUTH_KEY = StringUtils.fromString("mutualAuthentication");
    private static final BString JAAS_RELOAD_KEY = StringUtils.fromString("jaasConfigFileReloadEnabled");

    /**
     * Creates a KerberosConfig from a Ballerina map record.
     */
    public KerberosConfig(BMap<BString, Object> config) {
        this(
            config.getStringValue(SERVICE_NAME_KEY).getValue(),
            config.getStringValue(JAAS_LOGIN_CONTEXT_KEY).getValue(),
            config.getBooleanValue(MUTUAL_AUTH_KEY),
            config.getBooleanValue(JAAS_RELOAD_KEY)
        );
    }
}
