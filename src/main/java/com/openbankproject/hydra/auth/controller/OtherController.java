package com.openbankproject.hydra.auth.controller;

import com.openbankproject.hydra.auth.VO.AccountDataValue;
import com.openbankproject.hydra.auth.VO.SessionData;
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

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.HashMap;

@RestController
public class OtherController {
    // UK Open Banking
    @Value("${endpoint.path.prefix}/accounts")
    private String getAccountsUrl;
    
    @Value("${endpoint.path.prefix}/accounts/ACCOUNT_ID")
    private String getAccountUrl;
    
    @Value("${endpoint.path.prefix}/accounts/ACCOUNT_ID/balances")
    private String getBalanceUrl;
    
    @Value("${endpoint.path.prefix}/accounts/ACCOUNT_ID/transactions")
    private String getTransactionsUrl;

    // Berlin Group
    @Value("${obp.base_url}/berlin-group/v1.3/accounts")
    private String getBerlinGroupAccountsUrl;

    @Value("${obp.base_url}/berlin-group/v1.3/accounts/ACCOUNT_ID")
    private String getBerlinGroupAccountUrl;

    @Value("${obp.base_url}/berlin-group/v1.3/accounts/ACCOUNT_ID/balances")
    private String getBerlinGroupBalanceUrl;

    @Value("${obp.base_url}/berlin-group/v1.3/accounts/ACCOUNT_ID/transactions")
    private String getBerlinGroupTransactionsUrl;

    @Resource
    private RestTemplate restTemplate;

    @GetMapping("/account")
    public Object getAccounts(HttpSession session) {
        String accessToken = SessionData.getAccessToken(session);
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<AccountDataValue> exchange = restTemplate.exchange(getAccountsUrl, HttpMethod.GET, entity, AccountDataValue.class);
        return exchange.getBody().getData();
    }
    @GetMapping("/account/{accountId}")
    public Object getAccount(@PathVariable String accountId, HttpSession session) {
        String accessToken = SessionData.getAccessToken(session);
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<HashMap> exchange = restTemplate.exchange(getAccountUrl.replace("ACCOUNT_ID", accountId), HttpMethod.GET, entity, HashMap.class);
        return  exchange.getBody();
    }

    @GetMapping("/balances/account_id/{accountId}")
    public Object getBalances(@PathVariable String accountId, HttpSession session) {
        String accessToken = SessionData.getAccessToken(session);
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<HashMap> exchange = restTemplate.exchange(getBalanceUrl.replace("ACCOUNT_ID", accountId), HttpMethod.GET, entity, HashMap.class);
        return exchange.getBody();
    }
    @GetMapping("/transactions/account_id/{accountId}")
    public Object getTransactions(@PathVariable String accountId, HttpSession session) {
        String accessToken = SessionData.getAccessToken(session);
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<HashMap> exchange = restTemplate.exchange(getTransactionsUrl.replace("ACCOUNT_ID", accountId), HttpMethod.GET, entity,  HashMap.class);
        return exchange.getBody();
    }
    
    
    // Berlin Group
    @GetMapping("/account_bg")
    public Object getAccountsBerlinGroup(HttpSession session) {
        String consentId = SessionData.getConsentId(session);
        HttpHeaders headers = new HttpHeaders();
        headers.add("Consent-ID", consentId);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<HashMap> exchange = restTemplate.exchange(getBerlinGroupAccountsUrl, HttpMethod.GET, entity, HashMap.class);
        return exchange.getBody();
    }
    @GetMapping("/account_bg/{accountId}")
    public Object getAccountBerlinGroup(@PathVariable String accountId, HttpSession session) {
        String consentId = SessionData.getConsentId(session);
        HttpHeaders headers = new HttpHeaders();
        headers.add("Consent-ID", consentId);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<HashMap> exchange = restTemplate.exchange(getBerlinGroupAccountUrl.replace("ACCOUNT_ID", accountId), HttpMethod.GET, entity, HashMap.class);
        return  exchange.getBody();
    }
    @GetMapping("/balances_bg/account_id/{accountId}")
    public Object getBalanceBerlinGroups(@PathVariable String accountId, HttpSession session) {
        String consentId = SessionData.getConsentId(session);
        HttpHeaders headers = new HttpHeaders();
        headers.add("Consent-ID", consentId);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<HashMap> exchange = restTemplate.exchange(getBerlinGroupBalanceUrl.replace("ACCOUNT_ID", accountId), HttpMethod.GET, entity, HashMap.class);
        return exchange.getBody();
    }
    @GetMapping("/transactions_bg/account_id/{accountId}")
    public Object getTransactionsBerlinGroup(@PathVariable String accountId, HttpSession session) {
        String consentId = SessionData.getConsentId(session);
        HttpHeaders headers = new HttpHeaders();
        headers.add("Consent-ID", consentId);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<HashMap> exchange = restTemplate.exchange(getBerlinGroupTransactionsUrl.replace("ACCOUNT_ID", accountId), HttpMethod.GET, entity,  HashMap.class);
        return exchange.getBody();
    }

    @ExceptionHandler
    void handleIllegalArgumentException(Exception e, HttpServletResponse response) throws IOException {
        String errorMsg = e.getMessage().replaceFirst(".*?\\[(.*)\\]", "$1");
        response.getWriter().write(errorMsg);
    }
}
