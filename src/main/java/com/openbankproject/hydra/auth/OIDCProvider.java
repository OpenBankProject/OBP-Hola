package com.openbankproject.hydra.auth;

import java.util.Map;

/**
 * Common interface for OIDC providers (Hydra, OBP-OIDC, etc.)
 */
public interface OIDCProvider {
    /**
     * Whether the client uses private_key_jwt authentication.
     */
    boolean isPublicClient();

    /**
     * Build a client_assertion JWT for token endpoint authentication.
     * @throws UnsupportedOperationException if the provider does not support private_key_jwt
     */
    String buildClientAssertion();

    /**
     * Build a signed request object for the authorization endpoint.
     * @throws UnsupportedOperationException if the provider does not support request objects
     */
    String buildRequestObject(Map<String, String> queryParam);
}
