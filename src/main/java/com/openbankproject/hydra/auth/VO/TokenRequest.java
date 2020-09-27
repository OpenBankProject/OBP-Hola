package com.openbankproject.hydra.auth.VO;

public class TokenRequest {
    private final String grant_type = "authorization_code";
    private String code;
    private String redirect_uri;
    private String client_id;
    private String client_secret;

    public TokenRequest(String code, String redirect_uri, String client_id, String client_secret) {
        this.code = code;
        this.redirect_uri = redirect_uri;
        this.client_id = client_id;
        this.client_secret = client_secret;
    }

    public String getGrant_type() {
        return grant_type;
    }

    public String getCode() {
        return code;
    }

    public String getRedirect_uri() {
        return redirect_uri;
    }

    public String getClient_id() {
        return client_id;
    }

    public String getClient_secret() {
        return client_secret;
    }
}
