package com.openbankproject.model;


import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

class Account{
    @JsonProperty("iban")
    String iban;
    
    public String getIban() { return this.iban; }
    public void setIban(String iban) { this.iban = iban; }
}

class Balance{
    @JsonProperty("iban")
    String iban;
    
    public String getIban() { return this.iban; }
    public void setIban(String iban) { this.iban = iban; }
}

class Transaction{
    @JsonProperty("iban")
    String iban;
    
    public String getIban() { return this.iban; }
    public void setIban(String iban) { this.iban = iban; }
}

@JsonInclude(JsonInclude.Include.NON_NULL)
class Access{
    @JsonProperty("accounts")
    List<Account> accounts;
    @JsonProperty("balances")
    List<Balance> balances;
    @JsonProperty("transactions")
    List<Transaction> transactions;
    
    public Access(List<Account> accounts) {
        this.accounts = accounts;
    }
    public Access(List<Account> accounts, List<Balance> balances) {
        this.balances = balances;
    }
    public Access(List<Account> accounts, List<Balance> balances, List<Transaction> transactions) {
        this.transactions = transactions;
    }
    
    public List<Account> getAccounts() { return this.accounts; }
    public void setAccounts(List<Account> accounts) { this.accounts = accounts; }
    public List<Balance> getBalances() { return balances; }
    public void setBalances(List<Balance> balances) { this.balances = balances; }
    public List<Transaction> getTransactions() { return transactions; }
    public void setTransactions(List<Transaction> transactions) { this.transactions = transactions; }
}

public class PostConsentJson{
    Access access;
    boolean recurringIndicator;
    String validUntil;
    int frequencyPerDay;
    boolean combinedServiceIndicator;
    public PostConsentJson(String[] selectedObpScopes,
                           String[] ibans, 
                           boolean recurringIndicator, 
                           String validUntil, 
                           int frequencyPerDay, 
                           boolean combinedServiceIndicator) {
        
        List<Account> accounts = new ArrayList<>();
        for (String iban : ibans) {
            Account account = new Account();
            account.setIban(iban);
            accounts.add(account);
        }
        
        List<Balance> balances = new ArrayList<>();
        for (String iban : ibans) {
            Balance balance = new Balance();
            balance.setIban(iban);
            balances.add(balance);
        }
        
        List<Transaction> transactions = new ArrayList<>();
        for (String iban : ibans) {
            Transaction transaction = new Transaction();
            transaction.setIban(iban);
            transactions.add(transaction);
        }
        // List<Account> accounts = Arrays.asList(new Account[]{account});
        if(Arrays.asList(selectedObpScopes).contains("ReadAccountsBerlinGroup")) this.access = new Access(accounts);
        if(Arrays.asList(selectedObpScopes).contains("ReadBalancesBerlinGroup")) {
            if(this.access != null) {
                this.access.setBalances(balances);
            } else {
                this.access = new Access(null, balances);
            }
        }
        if(Arrays.asList(selectedObpScopes).contains("ReadTransactionsBerlinGroup")) {
            if(this.access != null) {
                this.access.setTransactions(transactions);
            } else {
                this.access = new Access(null, null, transactions);
            }
        }
        this.recurringIndicator = recurringIndicator;
        this.validUntil = validUntil;
        this.frequencyPerDay = frequencyPerDay;
        this.combinedServiceIndicator = combinedServiceIndicator;
    }

    @JsonProperty("access")
    public Access getAccess() {
        return this.access; }

    public void setAccess(Access access) {
        this.access = access; }

    @JsonProperty("recurringIndicator")
    public boolean getRecurringIndicator() {
        return this.recurringIndicator; }

    public void setRecurringIndicator(boolean recurringIndicator) {
        this.recurringIndicator = recurringIndicator; }

    @JsonProperty("validUntil")
    public String getValidUntil() {
        return this.validUntil; }

    public void setValidUntil(String validUntil) {
        this.validUntil = validUntil; }

    @JsonProperty("frequencyPerDay")
    public int getFrequencyPerDay() {
        return this.frequencyPerDay; }

    public void setFrequencyPerDay(int frequencyPerDay) {
        this.frequencyPerDay = frequencyPerDay; }

    @JsonProperty("combinedServiceIndicator")
    public boolean getCombinedServiceIndicator() {
        return this.combinedServiceIndicator; }

    public void setCombinedServiceIndicator(boolean combinedServiceIndicator) {
        this.combinedServiceIndicator = combinedServiceIndicator; }
}

