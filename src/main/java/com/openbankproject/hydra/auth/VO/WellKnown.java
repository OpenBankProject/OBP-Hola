package com.openbankproject.hydra.auth.VO;

/**
 * hydra wellknown endpoint response structure
 */

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class WellKnown {

    @JsonProperty("issuer")
    public String issuer;
    @JsonProperty("authorization_endpoint")
    public String authorizationEndpoint;
    @JsonProperty("token_endpoint")
    public String tokenEndpoint;
    @JsonProperty("jwks_uri")
    public String jwksUri;
    @JsonProperty("subject_types_supported")
    public List<String> subjectTypesSupported = null;
    @JsonProperty("response_types_supported")
    public List<String> responseTypesSupported = null;
    @JsonProperty("claims_supported")
    public List<String> claimsSupported = null;
    @JsonProperty("grant_types_supported")
    public List<String> grantTypesSupported = null;
    @JsonProperty("response_modes_supported")
    public List<String> responseModesSupported = null;
    @JsonProperty("userinfo_endpoint")
    public String userinfoEndpoint;
    @JsonProperty("scopes_supported")
    public List<String> scopesSupported = null;
    @JsonProperty("token_endpoint_auth_methods_supported")
    public List<String> tokenEndpointAuthMethodsSupported = null;
    @JsonProperty("userinfo_signing_alg_values_supported")
    public List<String> userinfoSigningAlgValuesSupported = null;
    @JsonProperty("id_token_signing_alg_values_supported")
    public List<String> idTokenSigningAlgValuesSupported = null;
    @JsonProperty("request_parameter_supported")
    public Boolean requestParameterSupported;
    @JsonProperty("request_uri_parameter_supported")
    public Boolean requestUriParameterSupported;
    @JsonProperty("require_request_uri_registration")
    public Boolean requireRequestUriRegistration;
    @JsonProperty("claims_parameter_supported")
    public Boolean claimsParameterSupported;
    @JsonProperty("revocation_endpoint")
    public String revocationEndpoint;
    @JsonProperty("backchannel_logout_supported")
    public Boolean backchannelLogoutSupported;
    @JsonProperty("backchannel_logout_session_supported")
    public Boolean backchannelLogoutSessionSupported;
    @JsonProperty("frontchannel_logout_supported")
    public Boolean frontchannelLogoutSupported;
    @JsonProperty("frontchannel_logout_session_supported")
    public Boolean frontchannelLogoutSessionSupported;
    @JsonProperty("end_session_endpoint")
    public String endSessionEndpoint;

    public String getIssuer() {
        return issuer;
    }

    public String getAuthorizationEndpoint() {
        return authorizationEndpoint;
    }

    public String getTokenEndpoint() {
        return tokenEndpoint;
    }

    public String getJwksUri() {
        return jwksUri;
    }

    public List<String> getSubjectTypesSupported() {
        return subjectTypesSupported;
    }

    public List<String> getResponseTypesSupported() {
        return responseTypesSupported;
    }

    public List<String> getClaimsSupported() {
        return claimsSupported;
    }

    public List<String> getGrantTypesSupported() {
        return grantTypesSupported;
    }

    public List<String> getResponseModesSupported() {
        return responseModesSupported;
    }

    public String getUserinfoEndpoint() {
        return userinfoEndpoint;
    }

    public List<String> getScopesSupported() {
        return scopesSupported;
    }

    public List<String> getTokenEndpointAuthMethodsSupported() {
        return tokenEndpointAuthMethodsSupported;
    }

    public List<String> getUserinfoSigningAlgValuesSupported() {
        return userinfoSigningAlgValuesSupported;
    }

    public List<String> getIdTokenSigningAlgValuesSupported() {
        return idTokenSigningAlgValuesSupported;
    }

    public Boolean getRequestParameterSupported() {
        return requestParameterSupported;
    }

    public Boolean getRequestUriParameterSupported() {
        return requestUriParameterSupported;
    }

    public Boolean getRequireRequestUriRegistration() {
        return requireRequestUriRegistration;
    }

    public Boolean getClaimsParameterSupported() {
        return claimsParameterSupported;
    }

    public String getRevocationEndpoint() {
        return revocationEndpoint;
    }

    public Boolean getBackchannelLogoutSupported() {
        return backchannelLogoutSupported;
    }

    public Boolean getBackchannelLogoutSessionSupported() {
        return backchannelLogoutSessionSupported;
    }

    public Boolean getFrontchannelLogoutSupported() {
        return frontchannelLogoutSupported;
    }

    public Boolean getFrontchannelLogoutSessionSupported() {
        return frontchannelLogoutSessionSupported;
    }

    public String getEndSessionEndpoint() {
        return endSessionEndpoint;
    }
}
