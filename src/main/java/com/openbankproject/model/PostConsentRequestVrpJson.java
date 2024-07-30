package com.openbankproject.model;


import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;



public class PostConsentRequestVrpJson{
    @JsonProperty("from_account")
    FromAccount fromAccount;
    @JsonProperty("to_account")
    ToAccount toAccount;
    @JsonProperty("time_to_live")
    int timeToLiveInSeconds;
    @JsonProperty("valid_from")
    String validFrom;
    @JsonProperty("email")
    String email;
    @JsonProperty("phone_number")
    String phoneNumber;

    public PostConsentRequestVrpJson(FromAccount fromAccount, ToAccount toAccount, int timeToLiveInSeconds, String validFrom, String email, String phoneNumber) {
        this.fromAccount = fromAccount;
        this.toAccount = toAccount;
        this.timeToLiveInSeconds = timeToLiveInSeconds;
        this.validFrom = validFrom;
        this.email = email;
        this.phoneNumber = phoneNumber;
    }

    public FromAccount getFromAccount() {
        return fromAccount;
    }

    public void setFromAccount(FromAccount fromAccount) {
        this.fromAccount = fromAccount;
    }

    public ToAccount getToAccount() {
        return toAccount;
    }

    public void setToAccount(ToAccount toAccount) {
        this.toAccount = toAccount;
    }

    public int getTimeToLiveInSeconds() {
        return timeToLiveInSeconds;
    }

    public void setTimeToLiveInSeconds(int timeToLiveInSeconds) {
        this.timeToLiveInSeconds = timeToLiveInSeconds;
    }

    public String getValidFrom() {
        return validFrom;
    }

    public void setValidFrom(String validFrom) {
        this.validFrom = validFrom;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }
}

