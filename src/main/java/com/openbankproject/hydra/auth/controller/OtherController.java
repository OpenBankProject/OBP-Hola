package com.openbankproject.hydra.auth.controller;

import com.openbankproject.hydra.auth.VO.AccountDataValue;
import com.openbankproject.hydra.auth.VO.SessionData;
import com.openbankproject.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.HashMap;

@RestController
public class OtherController {
    private static final Logger logger = LoggerFactory.getLogger(OtherController.class);
    
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
    
    @Value("${obp.base_url}/berlin-group/v1.3/payments/sepa-credit-transfers")
    private String initiatePaymentBerlinGroupUrl;
    
    // OBP
    @Value("${obp.base_url}/obp/v5.1.0/banks/BANK_ID/accounts/private")
    private String getObpAccountsUrl;

    @Value("${obp.base_url}/obp/v5.1.0/my/banks/BANK_ID/accounts/ACCOUNT_ID/account")
    private String getCoreAccountById;
    
    @Value("${obp.base_url}/obp/v5.1.0/banks/BANK_ID/accounts/ACCOUNT_ID/views/VIEW_ID")
    private String getCoreAccountByIdThroughView;
    
    @Value("${obp.base_url}/obp/v5.1.0/banks/BANK_ID/accounts/ACCOUNT_ID/balances")
    private String getBankAccountBalances;  
    
    @Value("${obp.base_url}/obp/v5.1.0/banks/BANK_ID/accounts/ACCOUNT_ID/views/VIEW_ID/balances")
    private String getBankAccountBalancesThroughView;
    
    @Value("${obp.base_url}/obp/v5.1.0/my/banks/BANK_ID/accounts/ACCOUNT_ID/transactions")
    private String getCoreTransactionsForBankAccount;
    
    @Value("${obp.base_url}/obp/v5.1.0/banks/BANK_ID/accounts/ACCOUNT_ID/VIEW_ID/transactions")
    private String getTransactionsForBankAccount;
    
    @Value("${obp.base_url}/obp/v5.1.0/consumer/current/consents/CONSENT_ID")
    private String getConsentByConsentId;
    
    @Value("${obp.base_url}/obp/v5.1.0/my/consent/current")
    private String selfRevokeConsentUrl;
    
    @Value("${obp.base_url}/obp/v5.1.0/my/mtls/certificate/current")
    private String mtlsClientCertificateInfo;


    @Value("${obp.base_url}/obp/v5.1.0/banks/BANK_ID/accounts/ACCOUNT_ID/VIEW_ID/transaction-request-types/COUNTERPARTY/transaction-requests")
    private String makePaymentCouterpartyObp;

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
        logger.debug("Consent-ID: " + consentId);

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
    @GetMapping("/initiate_payment_bg/{creditorIban}/{creditorName}/{debtorIban}/{amount}/{currency}")
    public Object initiatePaymentBerlinGroupUrl(@PathVariable String creditorIban,
                                                @PathVariable String creditorName,
                                                @PathVariable String debtorIban,
                                                @PathVariable String amount,
                                                @PathVariable String currency,
                                                HttpSession session) {
        String consentId = SessionData.getConsentId(session);
        HttpHeaders headers = new HttpHeaders();
        headers.add("Consent-ID", consentId);
        
        SepaCreditTransfersBerlinGroupV13 body = 
                new SepaCreditTransfersBerlinGroupV13(
                        new DebtorAccount(debtorIban),
                        new InstructedAmount(currency, amount),
                        new CreditorAccount(creditorIban),
                        creditorName
                        );
        HttpEntity<SepaCreditTransfersBerlinGroupV13> request = new HttpEntity<>(body, headers);
        try {
            ResponseEntity<HashMap> response = restTemplate.exchange(initiatePaymentBerlinGroupUrl, HttpMethod.POST, request,  HashMap.class);
            return response.getBody();
        } catch (HttpClientErrorException e) {
            String error = "Sorry! Cannot initiate the payment.";
            logger.error(error, e);
            return error + System.lineSeparator() + e.getResponseBodyAsString();
        }
    }
    @GetMapping("/transactions_bg/account_id/{accountId}")
    public Object getTransactionsBerlinGroup(@PathVariable String accountId, HttpSession session) {
        String consentId = SessionData.getConsentId(session);
        HttpHeaders headers = new HttpHeaders();
        headers.add("Consent-ID", consentId);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<HashMap> exchange = restTemplate.exchange(getBerlinGroupTransactionsUrl.replace("ACCOUNT_ID", accountId), HttpMethod.GET, entity,  HashMap.class);
        logger.debug("getTransactionsBerlinGroup status:" + exchange.getStatusCode().toString());
        logger.debug("getTransactionsBerlinGroup body:" + exchange.getBody().toString());
        return exchange.getBody();
    }


    // Open Bank Project
    @GetMapping("/account_obp")
    public Object getAccountsObp(HttpSession session) {
        String consentId = SessionData.getConsentId(session);
        String bankId = SessionData.getBankId(session);
        HttpHeaders headers = new HttpHeaders();
        headers.add("Consent-Id", consentId);
        HttpEntity<String> entity = new HttpEntity<>(headers);
        logger.debug("Consent-Id: " + consentId);

        ResponseEntity<HashMap> exchange = restTemplate
                .exchange(getObpAccountsUrl.replace("BANK_ID", bankId), HttpMethod.GET, entity, HashMap.class);
        return exchange.getBody();
    }
    @GetMapping("/account_obp/{bankId}/{accountId}/{viewId}")
    public Object getAccountObp(@PathVariable String bankId, @PathVariable String accountId, @PathVariable String viewId, HttpSession session) {
        String consentId = SessionData.getConsentId(session);
        HttpHeaders headers = new HttpHeaders();
        headers.add("Consent-Id", consentId);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<HashMap> exchange = restTemplate
                .exchange(getCoreAccountByIdThroughView
                        .replace("ACCOUNT_ID", accountId)
                        .replace("BANK_ID", bankId)
                        .replace("VIEW_ID", viewId), HttpMethod.GET, entity, HashMap.class);
        return  exchange.getBody();
    }
    @GetMapping("/balances_obp/bank_id/{bankId}/account_id/{accountId}/view_id/{viewId}")
    public Object getBalanceObp(@PathVariable String bankId, @PathVariable String accountId, @PathVariable String viewId, HttpSession session) {
        String consentId = SessionData.getConsentId(session);
        HttpHeaders headers = new HttpHeaders();
        headers.add("Consent-Id", consentId);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<HashMap> exchange = restTemplate
                .exchange(getBankAccountBalancesThroughView.replace("ACCOUNT_ID", accountId)
                        .replace("VIEW_ID", viewId)
                        .replace("BANK_ID", bankId), HttpMethod.GET, entity, HashMap.class);
        return exchange.getBody();
    }
    @GetMapping("/transactions_obp/bank_id/{bankId}/account_id/{accountId}/view_id/{viewId}")
    public Object getTransactionsObp(@PathVariable String bankId, @PathVariable String accountId, @PathVariable String viewId, HttpSession session) {
        String consentId = SessionData.getConsentId(session);
        HttpHeaders headers = new HttpHeaders();
        headers.add("Consent-Id", consentId);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<HashMap> exchange = restTemplate
                .exchange(getTransactionsForBankAccount.replace("ACCOUNT_ID", accountId)
                        .replace("VIEW_ID", viewId)
                        .replace("BANK_ID", bankId), HttpMethod.GET, entity,  HashMap.class);
        return exchange.getBody();
    }
    
    @GetMapping("/payment_obp/{bankId}/{accountId}/{viewId}/{couterpartyId}/{currency}/{amount}/{description}/{chargePolicy}/{futureDate}")
    public ResponseEntity<Object> makePaymentObp(@PathVariable String bankId, 
                                 @PathVariable String accountId, 
                                 @PathVariable String viewId, 
                                 @PathVariable String couterpartyId, 
                                 @PathVariable String currency, 
                                 @PathVariable String amount, 
                                 @PathVariable String description, 
                                 @PathVariable String chargePolicy, 
                                 @PathVariable String futureDate, 
                                 HttpSession session) {
        String consentId = SessionData.getConsentId(session);
        HttpHeaders headers = new HttpHeaders();
        headers.add("Consent-Id", consentId);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        PostJsonCreateTransactionRequestCounterparty body = new PostJsonCreateTransactionRequestCounterparty(
                couterpartyId,
                currency,
                amount,
                description,
                chargePolicy,
                futureDate    
        );

        HttpEntity<PostJsonCreateTransactionRequestCounterparty> request = new HttpEntity<>(body, headers);
        String url = makePaymentCouterpartyObp
                .replace("ACCOUNT_ID", accountId)
                .replace("VIEW_ID", viewId)
                .replace("BANK_ID", bankId);

        // Create response headers
        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.add("Path-Of-Call", HttpMethod.POST + ": " + url);
        try {
            ResponseEntity<HashMap> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    request,
                    HashMap.class
            );
            return new ResponseEntity<>(response.getBody(), responseHeaders, response.getStatusCode());
        } catch (HttpClientErrorException e) {
            return new ResponseEntity<>(e.getResponseBodyAsString(), responseHeaders, e.getStatusCode());
        }
    }
    @GetMapping("/revoke_consent_obp")
    public Object revokeConsentObp(HttpSession session) {
        String consentId = SessionData.getConsentId(session);
        String bankId = SessionData.getBankId(session);
        HttpHeaders headers = new HttpHeaders();
        headers.add("Consent-Id", consentId);
        HttpEntity<String> entity = new HttpEntity<>(headers);
        logger.debug("Consent-Id: " + consentId);

        ResponseEntity<HashMap> exchange = restTemplate
                .exchange(selfRevokeConsentUrl, HttpMethod.DELETE, entity, HashMap.class);
        return exchange.getBody();
    }
    @GetMapping("/mtls_client_cert_info")
    public Object mtlsClientCertificateInfo(HttpSession session) {
        String consentId = SessionData.getConsentId(session);
        HttpHeaders headers = new HttpHeaders();
        headers.add("Consent-Id", consentId);
        HttpEntity<String> entity = new HttpEntity<>(headers);
        logger.debug("Consent-Id: " + consentId);

        ResponseEntity<HashMap> exchange = restTemplate
                .exchange(mtlsClientCertificateInfo, HttpMethod.GET, entity, HashMap.class);
        return exchange.getBody();
    }
    @GetMapping("/consent_info")
    public Object consentInfo(HttpSession session) {
        String consentId = SessionData.getConsentId(session);
        HttpHeaders headers = new HttpHeaders();
        headers.add("Consent-Id", consentId);
        HttpEntity<String> entity = new HttpEntity<>(headers);
        logger.debug("Consent-Id: " + consentId);

        ResponseEntity<HashMap> exchange = restTemplate
                .exchange(getConsentByConsentId.replace("CONSENT_ID", consentId), HttpMethod.GET, entity, HashMap.class);
        return exchange.getBody();
    }
    

    @ExceptionHandler
    void handleIllegalArgumentException(Exception e, HttpServletResponse response) throws IOException {
        String errorMsg = e.getMessage().replaceFirst(".*?\\[(.*)\\]", "$1");
        response.getWriter().write(errorMsg);
    }
}
