package com.openban.hydra.auth.controller;

import com.openban.hydra.auth.VO.Account;
import com.openban.hydra.auth.VO.AccountDataValue;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
public class OtherController {
    @Value("${obp.base_url}/mx-open-finance/v0.0.1/accounts")
    private String getAccountsUrl;
    @Value("${obp.base_url}/mx-open-finance/v0.0.1/accounts/ACCOUNT_ID/balances")
    private String getBalanceUrl;
    @Value("${obp.base_url}/mx-open-finance/v0.0.1/accounts/ACCOUNT_ID/transactions")
    private String getTransactionsUrl;

    @GetMapping("/accounts")
    public List<Account> getAccounts(HttpSession session) {
        RestTemplate restTemplate = new RestTemplate();
        String idToken = (String) session.getAttribute("idToken");
        String accessToken = (String) session.getAttribute("access_token");
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Authorization: Bearer " + accessToken);
        headers.add("Content-Type", "application/json");
        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<AccountDataValue> exchange = restTemplate.exchange(getAccountsUrl, HttpMethod.GET, entity, AccountDataValue.class);
        return exchange.getBody().getData().getAccount();
    }

    @GetMapping("/balances/account_id/{accountId}")
    public Object getBalances(@PathVariable String accountId, HttpSession session) {
        RestTemplate restTemplate = new RestTemplate();
        String idToken = (String) session.getAttribute("idToken");
        String accessToken = (String) session.getAttribute("access_token");
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Authorization: Bearer " + accessToken);
        headers.add("Content-Type", "application/json");
        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<HashMap> exchange = restTemplate.exchange(getBalanceUrl.replace("ACCOUNT_ID", accountId), HttpMethod.GET, entity, HashMap.class);
        Map<String, Object> balances = (Map<String, Object>) exchange.getBody().get("Data");
        if(balances != null) {
            return balances.get("Balance");
        } else {
            return new HashMap<String, Object>();
        }
    }
    @GetMapping("/transactions/account_id/{accountId}")
    public Object getTransactions(@PathVariable String accountId, HttpSession session) {
        RestTemplate restTemplate = new RestTemplate();
        String idToken = (String) session.getAttribute("idToken");
        String accessToken = (String) session.getAttribute("access_token");
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Authorization: Bearer " + accessToken);
        headers.add("Content-Type", "application/json");
        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<HashMap> exchange = restTemplate.exchange(getTransactionsUrl.replace("ACCOUNT_ID", accountId), HttpMethod.GET, entity,  HashMap.class);
        List<Object> data = (List<Object>) exchange.getBody().get("Data");
        if(data != null) {
            return data;
        } else {
            return new ArrayList<>();
        }

    }

    @ExceptionHandler
    void handleIllegalArgumentException(Exception e, HttpServletResponse response) throws IOException {
        String errorMsg = e.getMessage().replaceFirst(".*?\\[(.*)\\]", "$1");
        response.getWriter().write(errorMsg);
    }
}
