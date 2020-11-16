package com.openbankproject.hydra.auth.VO;


import com.fasterxml.jackson.annotation.JsonProperty;

public class ConsentPostBodyV310 {
    @JsonProperty("Data")
    private ConsentPostBodyDataV310 Data;
    @JsonProperty("Risk")
    private String Risk = "";


    public ConsentPostBodyV310(String bankId, String[] permissions, String transactionFromDateTime, String transactionToDateTime, String expirationDateTime) {
        Data = new ConsentPostBodyDataV310(bankId, permissions,transactionFromDateTime, transactionToDateTime, expirationDateTime);
    }
}

class ConsentPostBodyDataV310 {
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

    public ConsentPostBodyDataV310(String bankId, String[] permissions, String transactionFromDateTime, String transactionToDateTime, String expirationDateTime) {
        this.BankId = bankId;
        Permissions = permissions;
        TransactionFromDateTime = transactionFromDateTime;
        TransactionToDateTime = transactionToDateTime;
        ExpirationDateTime = expirationDateTime;
    }
}