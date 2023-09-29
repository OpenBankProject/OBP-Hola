package com.openbankproject.hydra.auth.VO;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.stream.Collectors;

public class Banks {
    private List<Bank> banks;

    public List<Bank> getBanks() {
        return banks.stream().sorted().collect(Collectors.toList());
    }

    public void setBanks(List<Bank> banks) {
        this.banks = banks;
    }
}

class Bank implements Comparable<Bank> {
    private String id;
    @JsonProperty("short_name")
    private String short_name;
    @JsonProperty("full_name")
    private String full_name;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getShort_name() {
        return short_name;
    }

    public void setShort_name(String short_name) {
        this.short_name = short_name;
    }

    public String getFull_name() {
        return full_name;
    }

    public void setFull_name(String full_name) {
        this.full_name = full_name;
    }

    @Override
    public int compareTo(Bank obj) {
        return ((this.full_name + this.id).toLowerCase().compareTo((obj.full_name + obj.id).toLowerCase()));
    }
}
