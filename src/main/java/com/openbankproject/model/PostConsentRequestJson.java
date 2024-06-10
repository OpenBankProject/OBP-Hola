package com.openbankproject.model;


import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

class Entitlement {
    @JsonProperty("bank_id")
    String bankId;
    @JsonProperty("role_name")
    String roleName;

    public String getBankId() {
        return bankId;
    }

    public void setBankId(String bankId) {
        this.bankId = bankId;
    }

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }
}

public class PostConsentRequestJson{
    @JsonProperty("everything")
    boolean everything;
    @JsonProperty("account_access")
    List<AccountAccess> accountAccesses;
    @JsonProperty("entitlements")
    List<Entitlement> entitlements;
    @JsonProperty("bank_id")
    String bankId;
    @JsonProperty("time_to_live")
    int timeToLiveInSeconds;
    @JsonProperty("valid_from")
    String validFrom;

    public String getBankId() {
        return bankId;
    }

    public void setBankId(String bankId) {
        this.bankId = bankId;
    }

    public PostConsentRequestJson(boolean everything, String bankId, int ttl, String validFrom) {
        this.everything = everything;
        
        List<Entitlement> entitlements = new ArrayList<>();
        this.entitlements = entitlements;
        
        List<AccountAccess> accountAccesses = new ArrayList<>();
        this.accountAccesses = accountAccesses;
        
        this.bankId = bankId;
        
        this.timeToLiveInSeconds = ttl;
        
        this.validFrom = validFrom;
    }
    public PostConsentRequestJson(boolean everything, String bankId, int ttl, String validFrom, List<AccountAccess> accountAccesses) {
        this.everything = everything;
        
        List<Entitlement> entitlements = new ArrayList<>();
        this.entitlements = entitlements;
        
        this.accountAccesses = accountAccesses;
        
        this.bankId = bankId;
        
        this.timeToLiveInSeconds = ttl;
        
        this.validFrom = validFrom;
    }
    
    public boolean getEverything() {
        return this.everything; 
    }
    public void setEverything(boolean everything) {
        this.everything = everything; 
    }
    
    public List<Entitlement> getEntitlements() { return this.entitlements; }
    public void setEntitlements(List<Entitlement> entitlements) { this.entitlements = entitlements; }
    
    public List<AccountAccess> getAccountAccesses() { return this.accountAccesses; }
    public void setAccountAccesses(List<AccountAccess> accountAccesses) { this.accountAccesses = accountAccesses; }
}

