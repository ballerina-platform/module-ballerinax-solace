package io.ballerina.lib.solace.smf.config.auth;

/**
 * Base interface for authentication configurations.
 * <p>
 * Permits three authentication methods:
 * <ul>
 *   <li>{@link BasicAuthConfig} - username/password authentication
 *   <li>{@link KerberosConfig} - Kerberos (GSS-KRB) authentication
 *   <li>{@link OAuth2Config} - OAuth 2.0/OIDC authentication
 * </ul>
 */
public sealed interface AuthConfig permits BasicAuthConfig, KerberosConfig, OAuth2Config {
}
