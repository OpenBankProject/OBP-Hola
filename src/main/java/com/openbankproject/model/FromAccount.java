package com.openbankproject.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class FromAccount {
    @JsonProperty("bank_routing")
    BankRouting bankRouting;
    @JsonProperty("branch_routing")
    BranchRouting branchRouting;
    @JsonProperty("account_routing")
    AccountRouting accountRouting;

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

    public FromAccount(BankRouting bankRouting, BranchRouting branchRouting, AccountRouting accountRouting) {
        this.bankRouting = bankRouting;
        this.branchRouting = branchRouting;
        this.accountRouting = accountRouting;
    }
}
