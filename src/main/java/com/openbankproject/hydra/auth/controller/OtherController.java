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
import org.springframework.http.MediaType;
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

    // bookingStatus is mandatory on this endpoint in the Berlin Group specification, and OBP-API
    // enforces it: no default, and it rejects anything but booked, pending or both with OBP-10034.
    // The call has never carried it, so it could not have succeeded -- until the consent fixes
    // above that failure was masked by an earlier one. "both" asks for booked and pending
    // together, which is the fullest answer and what a demo client wants to show.
    @Value("${obp.base_url}/berlin-group/v1.3/accounts/ACCOUNT_ID/transactions?bookingStatus=both")
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

    // UK Open Banking v3.1 and v4.0.1
    //
    // The consent is the whole credential: these calls carry no Authorization header at all, only
    // the consent the PSU authorised. That mirrors the Berlin Group and OBP-native flows below --
    // the app never holds, and never needs, an access token of the PSU's.
    //
    // UK Open Banking's own specification binds a consent to an OAuth2 access token (a consent_id
    // claim OBP-API reads via checkUKConsent), and OBP-API still accepts that. Presenting the
    // consent on its own is an OBP extension: OBP-API's authentication dispatcher resolves the
    // consent behind Consent-Id, sees it was created by the UK standard, and validates it through
    // applyUKRules -- same gates as the token path, PSU resolved from the consent itself.
    //
    // Consumer-Key is required alongside it: OBP-API matches the requesting consumer against the
    // one the consent was lodged with, and rejects a mismatch with OBP-35015.
    //
    // Note the capitalisation. `Consent-Id` is the OBP/UK spelling; `Consent-ID` is Berlin Group's
    // and routes into the BG consent path, which rejects a UK consent with OBP-35036.
    private HttpHeaders ukConsentHeaders(HttpSession session) {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Consent-Id", SessionData.getConsentId(session));
        headers.add("Consumer-Key", consumerKey);
        return headers;
    }

    @GetMapping("/account")
    public Object getAccounts(HttpSession session) {
        HttpEntity<String> entity = new HttpEntity<>(ukConsentHeaders(session));

        try {
            ResponseEntity<AccountDataValue> exchange = restTemplate.exchange(getAccountsUrl, HttpMethod.GET, entity, AccountDataValue.class);
            return exchange.getBody().getData();
        } catch (HttpClientErrorException e) {
            return passThroughObpError("getAccounts", e);
        }
    }
    @GetMapping("/account/{accountId}")
    public Object getAccount(@PathVariable String accountId, HttpSession session) {
        HttpEntity<String> entity = new HttpEntity<>(ukConsentHeaders(session));

        try {
            ResponseEntity<HashMap> exchange = restTemplate.exchange(getAccountUrl.replace("ACCOUNT_ID", accountId), HttpMethod.GET, entity, HashMap.class);
            return exchange.getBody();
        } catch (HttpClientErrorException e) {
            return passThroughObpError("getAccount", e);
        }
    }

    @GetMapping("/balances/account_id/{accountId}")
    public Object getBalances(@PathVariable String accountId, HttpSession session) {
        HttpEntity<String> entity = new HttpEntity<>(ukConsentHeaders(session));

        try {
            ResponseEntity<HashMap> exchange = restTemplate.exchange(getBalanceUrl.replace("ACCOUNT_ID", accountId), HttpMethod.GET, entity, HashMap.class);
            return exchange.getBody();
        } catch (HttpClientErrorException e) {
            return passThroughObpError("getBalances", e);
        }
    }
    @GetMapping("/transactions/account_id/{accountId}")
    public Object getTransactions(@PathVariable String accountId, HttpSession session) {
        HttpEntity<String> entity = new HttpEntity<>(ukConsentHeaders(session));

        try {
            ResponseEntity<HashMap> exchange = restTemplate.exchange(getTransactionsUrl.replace("ACCOUNT_ID", accountId), HttpMethod.GET, entity, HashMap.class);
            return exchange.getBody();
        } catch (HttpClientErrorException e) {
            return passThroughObpError("getTransactions", e);
        }
    }

    @GetMapping("/account_uk4")
    public Object getAccountsV401(HttpSession session) {
        HttpEntity<String> entity = new HttpEntity<>(ukConsentHeaders(session));

        try {
            ResponseEntity<AccountDataValue> exchange = restTemplate.exchange(getAccountsUrlV401, HttpMethod.GET, entity, AccountDataValue.class);
            return exchange.getBody().getData();
        } catch (HttpClientErrorException e) {
            return passThroughObpError("getAccountsV401", e);
        }
    }

    @GetMapping("/account_uk4/{accountId}")
    public Object getAccountV401(@PathVariable String accountId, HttpSession session) {
        HttpEntity<String> entity = new HttpEntity<>(ukConsentHeaders(session));

        try {
            ResponseEntity<HashMap> exchange = restTemplate.exchange(getAccountUrlV401.replace("ACCOUNT_ID", accountId), HttpMethod.GET, entity, HashMap.class);
            return exchange.getBody();
        } catch (HttpClientErrorException e) {
            return passThroughObpError("getAccountV401", e);
        }
    }

    @GetMapping("/balances_uk4/account_id/{accountId}")
    public Object getBalancesV401(@PathVariable String accountId, HttpSession session) {
        HttpEntity<String> entity = new HttpEntity<>(ukConsentHeaders(session));

        try {
            ResponseEntity<HashMap> exchange = restTemplate.exchange(getBalanceUrlV401.replace("ACCOUNT_ID", accountId), HttpMethod.GET, entity, HashMap.class);
            return exchange.getBody();
        } catch (HttpClientErrorException e) {
            return passThroughObpError("getBalancesV401", e);
        }
    }
    @GetMapping("/transactions_uk4/account_id/{accountId}")
    public Object getTransactionsV401(@PathVariable String accountId, HttpSession session) {
        HttpEntity<String> entity = new HttpEntity<>(ukConsentHeaders(session));

        try {
            ResponseEntity<HashMap> exchange = restTemplate.exchange(getTransactionsUrlV401.replace("ACCOUNT_ID", accountId), HttpMethod.GET, entity, HashMap.class);
            return exchange.getBody();
        } catch (HttpClientErrorException e) {
            return passThroughObpError("getTransactionsV401", e);
        }
    }

    /**
     * Forward an OBP-API client error to the browser verbatim, preserving its status code.
     *
     * Without this the exception reaches the generic handler and the caller sees a 500 with the
     * real cause lost -- which is precisely how a consent rejection used to disappear silently.
     * The body is passed through unparsed so the OBP error code survives intact for the page to
     * render: OBP-35005 (consent not AUTHORISED), OBP-35015 (consent belongs to another consumer),
     * OBP-35036 (consent belongs to another API standard), OBP-35004 (bad consent signature).
     */
    private ResponseEntity<String> passThroughObpError(String operation, HttpClientErrorException e) {
        logger.warn(operation + " failed: " + e.getStatusCode() + " " + e.getResponseBodyAsString());
        return ResponseEntity.status(e.getStatusCode())
                .contentType(MediaType.APPLICATION_JSON)
                .body(e.getResponseBodyAsString());
    }


    // Berlin Group
    //
    // Consent-ID is Berlin Group's spelling and is what routes the request into OBP-API's Berlin
    // Group consent path (`Consent-Id` is the OBP/UK spelling and routes elsewhere).
    //
    // Consumer-Key has to travel with it. OBP-API matches the requesting consumer against the one
    // the consent was lodged with; with no way to identify the caller the check sees consumer_id
    // NONE, cannot match, and answers OBP-35001 -- indistinguishable, from the browser, from a
    // consent that genuinely does not exist. The UK calls above and the OBP-native ones below have
    // always sent it; Berlin Group was the one standard here that did not.
    private HttpHeaders bgConsentHeaders(HttpSession session) {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Consent-ID", SessionData.getConsentId(session));
        headers.add("Consumer-Key", consumerKey);
        return headers;
    }

    @GetMapping("/account_bg")
    public Object getAccountsBerlinGroup(HttpSession session) {
        String consentId = SessionData.getConsentId(session);
        HttpEntity<String> entity = new HttpEntity<>(bgConsentHeaders(session));
        logger.debug("Consent-ID: " + consentId);

        try {
            ResponseEntity<HashMap> exchange = restTemplate.exchange(getBerlinGroupAccountsUrl, HttpMethod.GET, entity, HashMap.class);
            return exchange.getBody();
        } catch (HttpClientErrorException e) {
            // Pass OBP's own error through, as the UK calls do, rather than guessing at a cause.
            //
            // This used to answer 200 with a hint that asserted the consent "was never confirmed
            // via SCA and is still in status received". That was one possible cause presented as
            // the certain one, and it outlived the situation it described: Hola does drive SCA
            // now, and the same message was still shown for a consent that had completed it and
            // was valid -- for a while it was the only thing on screen while the real cause was a
            // missing Consumer-Key header. A guess that cannot be wrong out loud is worse than no
            // guess: OBP's own code says which of OBP-35001, OBP-35005, OBP-20017 it actually is.
            return passThroughObpError("getAccountsBerlinGroup", e);
        }
    }
    @GetMapping("/account_bg/{accountId}")
    public Object getAccountBerlinGroup(@PathVariable String accountId, HttpSession session) {
        HttpEntity<String> entity = new HttpEntity<>(bgConsentHeaders(session));

        try {
            ResponseEntity<HashMap> exchange = restTemplate.exchange(getBerlinGroupAccountUrl.replace("ACCOUNT_ID", accountId), HttpMethod.GET, entity, HashMap.class);
            return exchange.getBody();
        } catch (HttpClientErrorException e) {
            return passThroughObpError("getAccountBerlinGroup", e);
        }
    }
    @GetMapping("/balances_bg/account_id/{accountId}")
    public Object getBalanceBerlinGroups(@PathVariable String accountId, HttpSession session) {
        HttpEntity<String> entity = new HttpEntity<>(bgConsentHeaders(session));

        try {
            ResponseEntity<HashMap> exchange = restTemplate.exchange(getBerlinGroupBalanceUrl.replace("ACCOUNT_ID", accountId), HttpMethod.GET, entity, HashMap.class);
            return exchange.getBody();
        } catch (HttpClientErrorException e) {
            return passThroughObpError("getBalanceBerlinGroups", e);
        }
    }
    @GetMapping("/initiate_payment_bg/{creditorIban}/{creditorName}/{debtorIban}/{amount}/{currency}")
    public Object initiatePaymentBerlinGroupUrl(@PathVariable String creditorIban,
                                                @PathVariable String creditorName,
                                                @PathVariable String debtorIban,
                                                @PathVariable String amount,
                                                @PathVariable String currency,
                                                HttpSession session) {
        HttpHeaders headers = bgConsentHeaders(session);

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
            // Was: a prose sentence with the response body glued on, returned as 200. That threw
            // away the status code and gave the browser something that is not JSON to parse.
            return passThroughObpError("initiatePaymentBerlinGroup", e);
        }
    }
    @GetMapping("/transactions_bg/account_id/{accountId}")
    public Object getTransactionsBerlinGroup(@PathVariable String accountId, HttpSession session) {
        HttpEntity<String> entity = new HttpEntity<>(bgConsentHeaders(session));

        try {
            ResponseEntity<HashMap> exchange = restTemplate.exchange(getBerlinGroupTransactionsUrl.replace("ACCOUNT_ID", accountId), HttpMethod.GET, entity,  HashMap.class);
            logger.debug("getTransactionsBerlinGroup status:" + exchange.getStatusCode().toString());
            logger.debug("getTransactionsBerlinGroup body:" + exchange.getBody().toString());
            return exchange.getBody();
        } catch (HttpClientErrorException e) {
            return passThroughObpError("getTransactionsBerlinGroup", e);
        }
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
