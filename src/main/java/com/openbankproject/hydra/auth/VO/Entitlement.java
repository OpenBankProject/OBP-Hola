package com.openbankproject.hydra.auth.VO;

public class Entitlement {
    private String entitlement_id;
    private String role_name;
    private String bank_id;

    public String getEntitlement_id() {
        return entitlement_id;
    }

    public void setEntitlement_id(String entitlement_id) {
        this.entitlement_id = entitlement_id;
    }

    public String getRole_name() {
        return role_name;
    }

    public void setRole_name(String role_name) {
        this.role_name = role_name;
    }

    public String getBank_id() {
        return bank_id;
    }

    public void setBank_id(String bank_id) {
        this.bank_id = bank_id;
    }
}
