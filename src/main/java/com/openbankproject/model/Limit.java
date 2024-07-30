package com.openbankproject.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class Limit {
    @JsonProperty("currency")
    String currency;
    @JsonProperty("max_single_amount")
    int maxSingleAmount;
    @JsonProperty("max_monthly_amount")
    int maxMonthlyAmount;
    @JsonProperty("max_number_of_monthly_transactions")
    int maxNumberOfMonthlyTransactions;
    @JsonProperty("max_yearly_amount")
    int maxYearlyAmount;
    @JsonProperty("max_number_of_yearly_transactions")
    int maxNumberOfYearlyTransactions;

    public Limit(String currency, int maxSingleAmount, int maxMonthlyAmount, int maxNumberOfMonthlyTransactions, int maxYearlyAmount, int maxNumberOfYearlyTransactions) {
        this.currency = currency;
        this.maxSingleAmount = maxSingleAmount;
        this.maxMonthlyAmount = maxMonthlyAmount;
        this.maxNumberOfMonthlyTransactions = maxNumberOfMonthlyTransactions;
        this.maxYearlyAmount = maxYearlyAmount;
        this.maxNumberOfYearlyTransactions = maxNumberOfYearlyTransactions;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public int getMaxSingleAmount() {
        return maxSingleAmount;
    }

    public void setMaxSingleAmount(int maxSingleAmount) {
        this.maxSingleAmount = maxSingleAmount;
    }

    public int getMaxMonthlyAmount() {
        return maxMonthlyAmount;
    }

    public void setMaxMonthlyAmount(int maxMonthlyAmount) {
        this.maxMonthlyAmount = maxMonthlyAmount;
    }

    public int getMaxNumberOfMonthlyTransactions() {
        return maxNumberOfMonthlyTransactions;
    }

    public void setMaxNumberOfMonthlyTransactions(int maxNumberOfMonthlyTransactions) {
        this.maxNumberOfMonthlyTransactions = maxNumberOfMonthlyTransactions;
    }

    public int getMaxYearlyAmount() {
        return maxYearlyAmount;
    }

    public void setMaxYearlyAmount(int maxYearlyAmount) {
        this.maxYearlyAmount = maxYearlyAmount;
    }

    public int getMaxNumberOfYearlyTransactions() {
        return maxNumberOfYearlyTransactions;
    }

    public void setMaxNumberOfYearlyTransactions(int maxNumberOfYearlyTransactions) {
        this.maxNumberOfYearlyTransactions = maxNumberOfYearlyTransactions;
    }
}
