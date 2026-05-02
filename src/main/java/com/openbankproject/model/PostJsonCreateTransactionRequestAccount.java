package com.openbankproject.model;

import com.fasterxml.jackson.annotation.JsonProperty;

class TransactionRequestAccountBody {
    @JsonProperty("bank_id")
    private String bankId;

    @JsonProperty("account_id")
    private String accountId;

    public String getBankId() {
        return bankId;
    }

    public void setBankId(String bankId) {
        this.bankId = bankId;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }
}


public class PostJsonCreateTransactionRequestAccount {
    @JsonProperty("to")
    private TransactionRequestAccountBody to;

    @JsonProperty("value")
    private Value value;

    @JsonProperty("description")
    private String description;

    @JsonProperty("charge_policy")
    private String chargePolicy;

    @JsonProperty("future_date")
    private String futureDate;

    public PostJsonCreateTransactionRequestAccount(String toBankId,
                                                        String toAccountId,
                                                        String currency,
                                                        String amount,
                                                        String description,
                                                        String chargePolicy,
                                                        String futureDate) {
        TransactionRequestAccountBody to = new TransactionRequestAccountBody();
        to.setBankId(toBankId);
        to.setAccountId(toAccountId);
        this.to = to;

        Value value = new Value();
        value.setCurrency(currency);
        value.setAmount(amount);
        this.value = value;

        this.description = description;
        this.chargePolicy = chargePolicy;
        this.futureDate = futureDate;


    }

    public TransactionRequestAccountBody getTo() {
        return to;
    }


    public void setTo(TransactionRequestAccountBody to) {
        this.to = to;
    }

    public Value getValue() {
        return value;
    }

    public void setValue(Value value) {
        this.value = value;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getChargePolicy() {
        return chargePolicy;
    }

    public void setChargePolicy(String chargePolicy) {
        this.chargePolicy = chargePolicy;
    }

    public String getFutureDate() {
        return futureDate;
    }

    public void setFutureDate(String futureDate) {
        this.futureDate = futureDate;
    }
}
