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

    // UK Open Banking v4.0.1
    @Value("${endpoint.path.prefix.v401}/accounts")
    private String getAccountsUrlV401;

    @Value("${endpoint.path.prefix.v401}/accounts/ACCOUNT_ID")
    private String getAccountUrlV401;

    @Value("${endpoint.path.prefix.v401}/accounts/ACCOUNT_ID/balances")
    private String getBalanceUrlV401;

    @Value("${endpoint.path.prefix.v401}/accounts/ACCOUNT_ID/transactions")
    private String getTransactionsUrlV401;

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
    
    @Value("${obp.base_url}/obp/v5.1.0/banks/BANK_ID/accounts/ACCOUNT_ID/VIEW_ID/counterparties")
    private String getExplicitCounterpartiesForAccount;
    
    @Value("${obp.base_url}/obp/v5.1.0/banks/BANK_ID/accounts/ACCOUNT_ID/VIEW_ID/transaction-request-types/COUNTERPARTY/transaction-requests")
    private String makePaymentCouterpartyObp;
    
    @Value("${obp.base_url}/obp/v5.1.0/banks/BANK_ID/accounts/ACCOUNT_ID/VIEW_ID/transaction-request-types/SEPA/transaction-requests")
    private String makePaymentSepaObp;

    @Value("${obp.base_url}/obp/v5.1.0/banks/BANK_ID/accounts/ACCOUNT_ID/VIEW_ID/transaction-request-types/ACCOUNT/transaction-requests")
    private String makePaymentAccountObp;

    @Value("${oauth2.client_id}")
    private String consumerKey;

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

    // UK Open Banking v4.0.1
    @GetMapping("/account_uk4")
    public Object getAccountsV401(HttpSession session) {
        String accessToken = SessionData.getAccessToken(session);
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<AccountDataValue> exchange = restTemplate.exchange(getAccountsUrlV401, HttpMethod.GET, entity, AccountDataValue.class);
        return exchange.getBody().getData();
    }
    @GetMapping("/account_uk4/{accountId}")
    public Object getAccountV401(@PathVariable String accountId, HttpSession session) {
        String accessToken = SessionData.getAccessToken(session);
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<HashMap> exchange = restTemplate.exchange(getAccountUrlV401.replace("ACCOUNT_ID", accountId), HttpMethod.GET, entity, HashMap.class);
        return  exchange.getBody();
    }

    @GetMapping("/balances_uk4/account_id/{accountId}")
    public Object getBalancesV401(@PathVariable String accountId, HttpSession session) {
        String accessToken = SessionData.getAccessToken(session);
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<HashMap> exchange = restTemplate.exchange(getBalanceUrlV401.replace("ACCOUNT_ID", accountId), HttpMethod.GET, entity, HashMap.class);
        return exchange.getBody();
    }
    @GetMapping("/transactions_uk4/account_id/{accountId}")
    public Object getTransactionsV401(@PathVariable String accountId, HttpSession session) {
        String accessToken = SessionData.getAccessToken(session);
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<HashMap> exchange = restTemplate.exchange(getTransactionsUrlV401.replace("ACCOUNT_ID", accountId), HttpMethod.GET, entity,  HashMap.class);
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

        try {
            ResponseEntity<HashMap> exchange = restTemplate.exchange(getBerlinGroupAccountsUrl, HttpMethod.GET, entity, HashMap.class);
            return exchange.getBody();
        } catch (HttpClientErrorException e) {
            // A freshly-created Berlin Group consent starts in status "received" and only becomes
            // usable after the SCA confirmation step OBP-API points to via the create-consent
            // response's _links.scaRedirect — a step Hola doesn't drive automatically. OBP-API's
            // account lookup rejects an unconfirmed consent with a generic "not found", so surface
            // what's actually going on instead of forwarding that as-is.
            logger.error("getAccountsBerlinGroup failed for Consent-ID " + consentId, e);
            HashMap<String, Object> error = new HashMap<>();
            error.put("code", e.getStatusCode().value());
            error.put("message", e.getResponseBodyAsString());
            error.put("hint", "This usually means the Berlin Group consent (Consent-ID: " + consentId
                    + ") was never confirmed via SCA after creation and is still in status 'received', "
                    + "so OBP-API won't use it yet. Hola's Berlin Group flow does not currently perform "
                    + "that SCA confirmation step.");
            return error;
        }
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
        headers.add("Consumer-Key", consumerKey);
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
        headers.add("Consumer-Key", consumerKey);
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
        headers.add("Consumer-Key", consumerKey);
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
        headers.add("Consumer-Key", consumerKey);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<HashMap> exchange = restTemplate
                .exchange(getTransactionsForBankAccount.replace("ACCOUNT_ID", accountId)
                        .replace("VIEW_ID", viewId)
                        .replace("BANK_ID", bankId), HttpMethod.GET, entity,  HashMap.class);
        return exchange.getBody();
    }
    
    @GetMapping("/counterparties_obp/bank_id/{bankId}/account_id/{accountId}/view_id/{viewId}")
    public Object getExplicitCounterpartiesForAccountObp(@PathVariable String bankId, @PathVariable String accountId, @PathVariable String viewId, HttpSession session) {
        String consentId = SessionData.getConsentId(session);
        HttpHeaders headers = new HttpHeaders();
        headers.add("Consent-Id", consentId);
        headers.add("Consumer-Key", consumerKey);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<HashMap> exchange = restTemplate
                .exchange(getExplicitCounterpartiesForAccount.replace("ACCOUNT_ID", accountId)
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
        headers.add("Consumer-Key", consumerKey);
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
    @GetMapping("/payment_obp_sepa/{bankId}/{accountId}/{viewId}/{iban}/{currency}/{amount}/{description}/{chargePolicy}/{futureDate}")
    public ResponseEntity<Object> makePaymentSepaObp(@PathVariable String bankId, 
                                 @PathVariable String accountId, 
                                 @PathVariable String viewId, 
                                 @PathVariable String iban, 
                                 @PathVariable String currency, 
                                 @PathVariable String amount, 
                                 @PathVariable String description, 
                                 @PathVariable String chargePolicy, 
                                 @PathVariable String futureDate, 
                                 HttpSession session) {
        String consentId = SessionData.getConsentId(session);
        HttpHeaders headers = new HttpHeaders();
        headers.add("Consent-Id", consentId);
        headers.add("Consumer-Key", consumerKey);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        PostJsonCreateTransactionRequestSepa body = new PostJsonCreateTransactionRequestSepa(
                iban,
                currency,
                amount,
                description,
                chargePolicy,
                futureDate    
        );

        HttpEntity<PostJsonCreateTransactionRequestSepa> request = new HttpEntity<>(body, headers);
        String url = makePaymentSepaObp
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
    @GetMapping("/payment_obp_account/{bankId}/{accountId}/{viewId}/{toBankId}/{toAccountId}/{currency}/{amount}/{description}/{chargePolicy}/{futureDate}")
    public ResponseEntity<Object> makePaymentAccountObp(@PathVariable String bankId,
                                 @PathVariable String accountId,
                                 @PathVariable String viewId,
                                 @PathVariable String toBankId,
                                 @PathVariable String toAccountId,
                                 @PathVariable String currency,
                                 @PathVariable String amount,
                                 @PathVariable String description,
                                 @PathVariable String chargePolicy,
                                 @PathVariable String futureDate,
                                 HttpSession session) {
        String consentId = SessionData.getConsentId(session);
        HttpHeaders headers = new HttpHeaders();
        headers.add("Consent-Id", consentId);
        headers.add("Consumer-Key", consumerKey);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        PostJsonCreateTransactionRequestAccount body = new PostJsonCreateTransactionRequestAccount(
                toBankId,
                toAccountId,
                currency,
                amount,
                description,
                chargePolicy,
                futureDate
        );

        HttpEntity<PostJsonCreateTransactionRequestAccount> request = new HttpEntity<>(body, headers);
        String url = makePaymentAccountObp
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
        headers.add("Consumer-Key", consumerKey);
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
        headers.add("Consumer-Key", consumerKey);
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
        headers.add("Consumer-Key", consumerKey);
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
