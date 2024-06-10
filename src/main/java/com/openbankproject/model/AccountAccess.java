package com.openbankproject.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class AccountAccess{
    @JsonProperty("account_routing")
    AccountRouting accountRouting;
    @JsonProperty("view_id")
    String viewId;

    public AccountRouting getAccountRouting() {
        return accountRouting;
    }

    public void setAccountRouting(AccountRouting accountRouting) {
        this.accountRouting = accountRouting;
    }

    public AccountAccess(AccountRouting accountRouting, String viewId) {
        this.viewId = viewId;
        this.accountRouting = accountRouting;
    }
}
