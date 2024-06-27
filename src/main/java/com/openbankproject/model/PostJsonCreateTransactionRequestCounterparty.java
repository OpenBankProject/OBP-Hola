package com.openbankproject.model;

import com.fasterxml.jackson.annotation.JsonProperty;

class CounterpartyId {
    @JsonProperty("counterparty_id")
    private String counterpartyId;

    public String getCounterpartyId() {
        return counterpartyId;
    }

    public void setCounterpartyId(String counterpartyId) {
        this.counterpartyId = counterpartyId;
    }
}


class Value {
    @JsonProperty("currency")
    private String currency;

    @JsonProperty("amount")
    private String amount;

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getAmount() {
        return amount;
    }

    public void setAmount(String amount) {
        this.amount = amount;
    }
}


public class PostJsonCreateTransactionRequestCounterparty {
    @JsonProperty("to")
    private CounterpartyId to;

    @JsonProperty("value")
    private Value value;

    @JsonProperty("description")
    private String description;

    @JsonProperty("charge_policy")
    private String chargePolicy;

    @JsonProperty("future_date")
    private String futureDate;
    
    public PostJsonCreateTransactionRequestCounterparty(String couterpartyId, 
                                                        String currency, 
                                                        String amount, 
                                                        String description, 
                                                        String chargePolicy,
                                                        String futureDate) {
        CounterpartyId to = new CounterpartyId();
        to.setCounterpartyId(couterpartyId); 
        this.to = to;
        
        Value value = new Value();
        value.setCurrency(currency);
        value.setAmount(amount);
        this.value = value;
        
        this.description = description;
        this.chargePolicy = chargePolicy;
        this.futureDate = futureDate;
        
        
    }

    public CounterpartyId getTo() {
        return to;
    }
    

    public void setTo(CounterpartyId to) {
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
