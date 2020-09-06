package com.openban.hydra.auth.VO;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Account {
    @JsonProperty("AccountId")
    private String AccountId;
    @JsonProperty("Nickname")
    private String Nickname;
    @JsonProperty("Currency")
    private String Currency;
    @JsonProperty("Account")
    private AccountName Account;

    public String getAccountId() {
        return AccountId;
    }

    public void setAccountId(String accountId) {
        AccountId = accountId;
    }

    public String getNickname() {
        return Nickname;
    }

    public void setNickname(String nickname) {
        Nickname = nickname;
    }

    public String getCurrency() {
        return Currency;
    }

    public void setCurrency(String currency) {
        Currency = currency;
    }

    public AccountName getAccount() {
        return Account;
    }

    public void setAccount(AccountName account) {
        Account = account;
    }
}
