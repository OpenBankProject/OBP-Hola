package com.openbankproject.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ToAccount {
    @JsonProperty("bank_routing")
    BankRouting bankRouting;
    @JsonProperty("branch_routing")
    BranchRouting branchRouting;
    @JsonProperty("account_routing")
    AccountRouting accountRouting;
    @JsonProperty("limit")
    Limit limit;

    public ToAccount(BankRouting bankRouting, BranchRouting branchRouting, AccountRouting accountRouting, Limit limit) {
        this.bankRouting = bankRouting;
        this.branchRouting = branchRouting;
        this.accountRouting = accountRouting;
        this.limit = limit;
    }

    public BankRouting getBankRouting() {
        return bankRouting;
    }

    public void setBankRouting(BankRouting bankRouting) {
        this.bankRouting = bankRouting;
    }

    public BranchRouting getBranchRouting() {
        return branchRouting;
    }

    public void setBranchRouting(BranchRouting branchRouting) {
        this.branchRouting = branchRouting;
    }

    public AccountRouting getAccountRouting() {
        return accountRouting;
    }

    public void setAccountRouting(AccountRouting accountRouting) {
        this.accountRouting = accountRouting;
    }

    public Limit getLimit() {
        return limit;
    }

    public void setLimit(Limit limit) {
        this.limit = limit;
    }
}
