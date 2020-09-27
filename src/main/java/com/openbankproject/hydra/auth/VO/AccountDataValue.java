package com.openbankproject.hydra.auth.VO;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

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
