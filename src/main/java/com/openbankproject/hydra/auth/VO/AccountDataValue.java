package com.openbankproject.hydra.auth.VO;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

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
    private List<Map<String, Object>> Account;

    public List<Map<String, Object>> getAccount() {
        return Account;
    }

    public void setAccount(List<Map<String, Object>> account) {
        Account = account;
    }
}