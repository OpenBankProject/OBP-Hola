package com.openbankproject.hydra.auth.VO;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class AccountData {
    @JsonProperty("Account")
    private List<Account> Account;

    public List<Account> getAccount() {
        return Account;
    }

    public void setAccount(List<Account> account) {
        Account = account;
    }
}
