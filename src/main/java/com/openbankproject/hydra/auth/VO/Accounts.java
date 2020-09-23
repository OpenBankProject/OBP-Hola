package com.openbankproject.hydra.auth.VO;

import java.util.stream.Stream;

public class Accounts {
    private AccountMini[] accounts;

    public AccountMini[] getAccounts() {
        return accounts;
    }

    public void setAccounts(AccountMini[] accounts) {
        this.accounts = accounts;
    }
    public String[] accountIds() {
        return Stream.of(accounts).map(AccountMini::getId).toArray(String[]::new);
    }
}
class AccountMini {
    private String id;
    private String label;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }
}
