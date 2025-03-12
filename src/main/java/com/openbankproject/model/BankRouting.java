package com.openbankproject.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class BankRouting {
    
    @JsonProperty("scheme")
    String scheme;
    public String getScheme() { return this.scheme; }
    public void setScheme(String scheme) { this.scheme = scheme; }
    @JsonProperty("address")
            
    String address;
    public String getAddress() { return this.address; }
    public void setAddress(String address) { this.address = address; }
    public BankRouting(String scheme, String address) {
        this.scheme = scheme;
        this.address = address;
    }
}
