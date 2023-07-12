package com.openbankproject.model;

import javax.annotation.Generated;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "debtorAccount",
        "instructedAmount",
        "creditorAccount",
        "creditorName"
})
@Generated("jsonschema2pojo")
public class SepaCreditTransfersBerlinGroupV13 {

    @JsonProperty("debtorAccount")
    private DebtorAccount debtorAccount;
    @JsonProperty("instructedAmount")
    private InstructedAmount instructedAmount;
    @JsonProperty("creditorAccount")
    private CreditorAccount creditorAccount;
    @JsonProperty("creditorName")
    private String creditorName;

    /**
     * No args constructor for use in serialization
     *
     */
    public SepaCreditTransfersBerlinGroupV13() {
    }

    /**
     *
     * @param debtorAccount
     * @param creditorName
     * @param creditorAccount
     * @param instructedAmount
     */
    public SepaCreditTransfersBerlinGroupV13(DebtorAccount debtorAccount, InstructedAmount instructedAmount, CreditorAccount creditorAccount, String creditorName) {
        super();
        this.debtorAccount = debtorAccount;
        this.instructedAmount = instructedAmount;
        this.creditorAccount = creditorAccount;
        this.creditorName = creditorName;
    }

    @JsonProperty("debtorAccount")
    public DebtorAccount getDebtorAccount() {
        return debtorAccount;
    }

    @JsonProperty("debtorAccount")
    public void setDebtorAccount(DebtorAccount debtorAccount) {
        this.debtorAccount = debtorAccount;
    }

    @JsonProperty("instructedAmount")
    public InstructedAmount getInstructedAmount() {
        return instructedAmount;
    }

    @JsonProperty("instructedAmount")
    public void setInstructedAmount(InstructedAmount instructedAmount) {
        this.instructedAmount = instructedAmount;
    }

    @JsonProperty("creditorAccount")
    public CreditorAccount getCreditorAccount() {
        return creditorAccount;
    }

    @JsonProperty("creditorAccount")
    public void setCreditorAccount(CreditorAccount creditorAccount) {
        this.creditorAccount = creditorAccount;
    }

    @JsonProperty("creditorName")
    public String getCreditorName() {
        return creditorName;
    }

    @JsonProperty("creditorName")
    public void setCreditorName(String creditorName) {
        this.creditorName = creditorName;
    }

}