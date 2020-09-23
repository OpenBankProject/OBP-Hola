package com.openbankproject.hydra.auth.VO;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AccountDataValue {
    @JsonProperty("Data")
    private AccountData Data;

    public AccountData getData() {
        return Data;
    }

    public void setData(AccountData data) {
        Data = data;
    }
}
class AccountData {
    @JsonProperty("Account")
    private List<Account> Account;

    public List<Account> getAccount() {
        return Account;
    }

    public void setAccount(List<Account> account) {
        Account = account;
    }
}

class Account {
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

class AccountName {
    @JsonProperty("SchemeName")
    private String SchemeName;
    @JsonProperty("Identification")
    private String Identification;
    @JsonProperty("Name")
    private String Name;

    public String getSchemeName() {
        return SchemeName;
    }

    public void setSchemeName(String schemeName) {
        SchemeName = schemeName;
    }

    public String getIdentification() {
        return Identification;
    }

    public void setIdentification(String identification) {
        Identification = identification;
    }

    public String getName() {
        return Name;
    }

    public void setName(String name) {
        Name = name;
    }
}