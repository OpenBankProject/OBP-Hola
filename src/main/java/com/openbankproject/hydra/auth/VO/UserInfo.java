package com.openbankproject.hydra.auth.VO;

import java.util.List;

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

class EntitlementList {
    private List<Entitlement> list;

    public List<Entitlement> getList() {
        return list;
    }

    public void setList(List<Entitlement> list) {
        this.list = list;
    }
}

class Entitlement {
    private String entitlement_id;
    private String role_name;
    private String bank_id;

    public String getEntitlement_id() {
        return entitlement_id;
    }

    public void setEntitlement_id(String entitlement_id) {
        this.entitlement_id = entitlement_id;
    }

    public String getRole_name() {
        return role_name;
    }

    public void setRole_name(String role_name) {
        this.role_name = role_name;
    }

    public String getBank_id() {
        return bank_id;
    }

    public void setBank_id(String bank_id) {
        this.bank_id = bank_id;
    }
}

class ViewList {
    private List<View> list;

    public List<View> getList() {
        return list;
    }

    public void setList(List<View> list) {
        this.list = list;
    }
}

class View {
    private String view_id;
    private String bank_id;
    private String account_id;

    public String getView_id() {
        return view_id;
    }

    public void setView_id(String view_id) {
        this.view_id = view_id;
    }

    public String getBank_id() {
        return bank_id;
    }

    public void setBank_id(String bank_id) {
        this.bank_id = bank_id;
    }

    public String getAccount_id() {
        return account_id;
    }

    public void setAccount_id(String account_id) {
        this.account_id = account_id;
    }
}
