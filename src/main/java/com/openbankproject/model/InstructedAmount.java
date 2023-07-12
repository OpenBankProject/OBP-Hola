package com.openbankproject.model;

import javax.annotation.Generated;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "currency",
        "amount"
})
@Generated("jsonschema2pojo")
public class InstructedAmount {

    @JsonProperty("currency")
    private String currency;
    @JsonProperty("amount")
    private String amount;

    /**
     * No args constructor for use in serialization
     *
     */
    public InstructedAmount() {
    }

    /**
     *
     * @param amount
     * @param currency
     */
    public InstructedAmount(String currency, String amount) {
        super();
        this.currency = currency;
        this.amount = amount;
    }

    @JsonProperty("currency")
    public String getCurrency() {
        return currency;
    }

    @JsonProperty("currency")
    public void setCurrency(String currency) {
        this.currency = currency;
    }

    @JsonProperty("amount")
    public String getAmount() {
        return amount;
    }

    @JsonProperty("amount")
    public void setAmount(String amount) {
        this.amount = amount;
    }

}