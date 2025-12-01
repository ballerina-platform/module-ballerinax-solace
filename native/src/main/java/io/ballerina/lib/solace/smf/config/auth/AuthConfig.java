package io.ballerina.lib.solace.smf.config.auth;

/**
 * Sealed interface representing authentication configurations.
 * Permits three authentication types:
 * - BasicAuthConfig - username/password authentication
 * - KerberosConfig - Kerberos (GSS-KRB) authentication
 * - OAuth2Config - OAuth 2.0/OIDC authentication
 */
public sealed interface AuthConfig permits BasicAuthConfig, KerberosConfig, OAuth2Config {
}