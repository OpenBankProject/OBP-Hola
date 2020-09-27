package com.openbankproject.hydra.auth.VO;

public class UserInfo {
    private String user_id;
    private String email;
    private String provider_id;
    private String provider;
    private String username;
    private EntitlementList entitlements;
    private ViewList views;

    public String getUser_id() {
        return user_id;
    }

    public void setUser_id(String user_id) {
        this.user_id = user_id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getProvider_id() {
        return provider_id;
    }

    public void setProvider_id(String provider_id) {
        this.provider_id = provider_id;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public EntitlementList getEntitlements() {
        return entitlements;
    }

    public void setEntitlements(EntitlementList entitlements) {
        this.entitlements = entitlements;
    }

    public ViewList getViews() {
        return views;
    }

    public void setViews(ViewList views) {
        this.views = views;
    }
}
