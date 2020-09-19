package com.openban.hydra.auth.VO;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class Banks {
    private List<Bank> banks;

    public List<Bank> getBanks() {
        return banks;
    }

    public void setBanks(List<Bank> banks) {
        this.banks = banks;
    }
}

class Bank {
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
}
