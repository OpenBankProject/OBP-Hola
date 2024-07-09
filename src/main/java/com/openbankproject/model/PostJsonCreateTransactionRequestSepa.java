package com.openbankproject.model;

import com.fasterxml.jackson.annotation.JsonProperty;

class Iban {
    @JsonProperty("iban")
    private String iban;

    public String getIban() {
        return iban;
    }

    public void setIban(String iban) {
        this.iban = iban;
    }
}


public class PostJsonCreateTransactionRequestSepa {
    @JsonProperty("to")
    private Iban to;

    @JsonProperty("value")
    private Value value;

    @JsonProperty("description")
    private String description;

    @JsonProperty("charge_policy")
    private String chargePolicy;

    @JsonProperty("future_date")
    private String futureDate;
    
    public PostJsonCreateTransactionRequestSepa(String iban, 
                                                        String currency, 
                                                        String amount, 
                                                        String description, 
                                                        String chargePolicy,
                                                        String futureDate) {
        Iban to = new Iban();
        to.setIban(iban); 
        this.to = to;
        
        Value value = new Value();
        value.setCurrency(currency);
        value.setAmount(amount);
        this.value = value;
        
        this.description = description;
        this.chargePolicy = chargePolicy;
        this.futureDate = futureDate;
        
        
    }

    public Iban getTo() {
        return to;
    }
    

    public void setTo(Iban to) {
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
