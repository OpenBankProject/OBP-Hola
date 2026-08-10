package com.openbankproject.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * The counterparty limit carried inside a VRP consent request's to_account.
 *
 * Field names, types and order mirror OBP-API's PostCounterpartyLimitV510. The amounts are
 * Strings there rather than numbers, and every field is mandatory -- omitting one, or sending an
 * amount as a JSON number, makes OBP-API reject the enclosing to_account object outright
 * ("No usable value for to_account").
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Limit {
    @JsonProperty("currency")
    String currency;
    @JsonProperty("max_single_amount")
    String maxSingleAmount;
    @JsonProperty("max_monthly_amount")
    String maxMonthlyAmount;
    @JsonProperty("max_number_of_monthly_transactions")
    int maxNumberOfMonthlyTransactions;
    @JsonProperty("max_yearly_amount")
    String maxYearlyAmount;
    @JsonProperty("max_number_of_yearly_transactions")
    int maxNumberOfYearlyTransactions;
    @JsonProperty("max_total_amount")
    String maxTotalAmount;
    @JsonProperty("max_number_of_transactions")
    int maxNumberOfTransactions;

    public Limit(String currency,
                 String maxSingleAmount,
                 String maxMonthlyAmount,
                 int maxNumberOfMonthlyTransactions,
                 String maxYearlyAmount,
                 int maxNumberOfYearlyTransactions,
                 String maxTotalAmount,
                 int maxNumberOfTransactions) {
        this.currency = currency;
        this.maxSingleAmount = maxSingleAmount;
        this.maxMonthlyAmount = maxMonthlyAmount;
        this.maxNumberOfMonthlyTransactions = maxNumberOfMonthlyTransactions;
        this.maxYearlyAmount = maxYearlyAmount;
        this.maxNumberOfYearlyTransactions = maxNumberOfYearlyTransactions;
        this.maxTotalAmount = maxTotalAmount;
        this.maxNumberOfTransactions = maxNumberOfTransactions;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getMaxSingleAmount() {
        return maxSingleAmount;
    }

    public void setMaxSingleAmount(String maxSingleAmount) {
        this.maxSingleAmount = maxSingleAmount;
    }

    public String getMaxMonthlyAmount() {
        return maxMonthlyAmount;
    }

    public void setMaxMonthlyAmount(String maxMonthlyAmount) {
        this.maxMonthlyAmount = maxMonthlyAmount;
    }

    public int getMaxNumberOfMonthlyTransactions() {
        return maxNumberOfMonthlyTransactions;
    }

    public void setMaxNumberOfMonthlyTransactions(int maxNumberOfMonthlyTransactions) {
        this.maxNumberOfMonthlyTransactions = maxNumberOfMonthlyTransactions;
    }

    public String getMaxYearlyAmount() {
        return maxYearlyAmount;
    }

    public void setMaxYearlyAmount(String maxYearlyAmount) {
        this.maxYearlyAmount = maxYearlyAmount;
    }

    public int getMaxNumberOfYearlyTransactions() {
        return maxNumberOfYearlyTransactions;
    }

    public void setMaxNumberOfYearlyTransactions(int maxNumberOfYearlyTransactions) {
        this.maxNumberOfYearlyTransactions = maxNumberOfYearlyTransactions;
    }

    public String getMaxTotalAmount() {
        return maxTotalAmount;
    }

    public void setMaxTotalAmount(String maxTotalAmount) {
        this.maxTotalAmount = maxTotalAmount;
    }

    public int getMaxNumberOfTransactions() {
        return maxNumberOfTransactions;
    }

    public void setMaxNumberOfTransactions(int maxNumberOfTransactions) {
        this.maxNumberOfTransactions = maxNumberOfTransactions;
    }
}
