package com.openbankproject.hydra.auth.VO;

import com.fasterxml.jackson.annotation.JsonProperty;

public class AccountName {
    @JsonProperty("SchemeName")
    private String SchemeName;
    @JsonProperty("Identification")
    private String Identification;
    @JsonProperty("Name")
    private String Name;

    public String getSchemeName() {
        return SchemeName;
    }

    public void setSchemeName(String schemeName) {
        SchemeName = schemeName;
    }

    public String getIdentification() {
        return Identification;
    }

    public void setIdentification(String identification) {
        Identification = identification;
    }

    public String getName() {
        return Name;
    }

    public void setName(String name) {
        Name = name;
    }
}
