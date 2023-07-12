package com.openbankproject.model;

import javax.annotation.Generated;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "iban"
})
@Generated("jsonschema2pojo")
public class CreditorAccount {

    @JsonProperty("iban")
    private String iban;

    /**
     * No args constructor for use in serialization
     *
     */
    public CreditorAccount() {
    }

    /**
     *
     * @param iban
     */
    public CreditorAccount(String iban) {
        super();
        this.iban = iban;
    }

    @JsonProperty("iban")
    public String getIban() {
        return iban;
    }

    @JsonProperty("iban")
    public void setIban(String iban) {
        this.iban = iban;
    }

}