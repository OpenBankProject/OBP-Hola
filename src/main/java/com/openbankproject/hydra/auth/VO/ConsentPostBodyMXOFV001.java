package com.openbankproject.hydra.auth.VO;


import com.fasterxml.jackson.annotation.JsonProperty;

public class ConsentPostBodyMXOFV001{
    @JsonProperty("Data")
    private ConsentPostBodyDataMXOFV001 Data;

    public ConsentPostBodyMXOFV001(String bankId, String[] permissions, String transactionFromDateTime, String transactionToDateTime, String expirationDateTime) {
        Data = new ConsentPostBodyDataMXOFV001(bankId, permissions,transactionFromDateTime, transactionToDateTime, expirationDateTime);
    }
}

class ConsentPostBodyDataMXOFV001 {
    @JsonProperty("TransactionToDateTime")
    private String TransactionToDateTime;
    @JsonProperty("ExpirationDateTime")
    private String ExpirationDateTime;
    @JsonProperty("Permissions")
    private String[] Permissions;
    @JsonProperty("TransactionFromDateTime")
    private String TransactionFromDateTime;
    @JsonProperty("BankId")
    private String BankId;

    public ConsentPostBodyDataMXOFV001(String bankId, String[] permissions, String transactionFromDateTime, String transactionToDateTime, String expirationDateTime) {
        this.BankId = bankId;
        Permissions = permissions;
        TransactionFromDateTime = transactionFromDateTime;
        TransactionToDateTime = transactionToDateTime;
        ExpirationDateTime = expirationDateTime;
    }
}