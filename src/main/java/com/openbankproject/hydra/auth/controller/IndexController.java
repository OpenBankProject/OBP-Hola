package com.openbankproject.hydra.auth.controller;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.JWTParser;
import com.openbankproject.RedisService;
import com.openbankproject.hydra.auth.VO.*;
import com.openbankproject.hydra.auth.util.PKCEUtil;
import com.openbankproject.model.*;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.ServletContextAware;

import javax.annotation.Resource;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Controller
public class IndexController implements ServletContextAware {
    private static final Logger logger = LoggerFactory.getLogger(IndexController.class);

    // if need idToken, add openid; if need refreshToken, add offline
    @Value("openid,offline,${oauth2.client_scope}")
    private LinkedHashSet<String> allScopes;

    @Value("${oauth2.redirect_uri}")
    private String redirectUri;
    @Value("${oauth2.client_id}")
    private String clientId;
    // default is empty string
    @Value("${oauth2.client_secret:}")
    private String clientSecret;

    @Value("${obp.base_url:#}")
    private String obpBaseUrl;
    @Value("${obp.base_url}/obp/v4.0.0/users/current")
    private String currentUserUrl;
    @Value("${obp.base_url}/obp/v4.0.0/banks")
    private String getBanksUrl;
    @Value("${endpoint.path.prefix}/account-access-consents")
    private String createConsentsUrl;
    @Value("${endpoint.path.prefix.v401}/account-access-consents")
    private String createConsentsUrlV401;
    @Value("${endpoint.path.prefix.v401}/account-access-consents/CONSENT_ID")
    private String getConsentInformationUrlV401;
    @Value("${obp.base_url}/berlin-group/v1.3/consents")
    private String createBerlinGroupConsentsUrl;
    @Value("${obp.base_url}/berlin-group/v1.3/consents/CONSENT_ID")
    private String getConsentInformationBerlinGroup;

    @Value("${obp.base_url}/obp/v5.1.0/consumer/consent-requests")
    private String createConsentRequest;
    
    @Value("${obp.base_url}/obp/v5.1.0/consumer/vrp-consent-requests")
    private String createConsentRequestVrp;
    
    @Value("${obp.base_url}/obp/v4.0.0/banks/BANK_ID/consents/CONSENT_ID")
    private String updateConsentStatusUrl;
    
    @Value("${obp.base_url}/obp/v5.1.0/consumer/consent-requests/CONSENT_REQUEST_ID/consents")
    private String getConsentByConsentRequestId;

    @Value("${display_standards}")
    private String displayStandards;
    @Value("${button.background_color:#c9302c}")
    private String buttonBackgroundColor;
    @Value("${button.hover.background_color:#b92c28}")
    private String buttonHoverBackgroundColor;
    @Value("${logo.bank.enabled:false}")
    private String showBankLogo;
    @Value("${logo.bank.url:#}")
    private String bankLogoUrl;

    @Value("${show_unhandled_errors:false}")
    private boolean showUnhandledErrors;

    @Resource
    private RestTemplate restTemplate;
    @Resource
    private WellKnown openIDConfiguration;

    /**
     * initiate global variable
     * @param servletContext
     */
    @Override
    public void setServletContext(ServletContext servletContext) {
        servletContext.setAttribute("obp_url", obpBaseUrl);
    }


    @GetMapping({ "/", "/index", "index.html"})
    public String index(@RequestHeader(required = false, name = "Referer") String referer, Model model) {
        if (!returnErrorIfAny(referer).isEmpty()) {
            model.addAttribute("errorMsg",  returnErrorIfAny(referer));
            return "error";
        }
        String[] apiStandards = displayStandards.split(",");
        String[] displayStandards = apiStandards;
        if(apiStandards.length == 1 && apiStandards[0].trim().isEmpty()) {
            displayStandards = new String[] {"display_standards=UKOpenBanking,UKOpenBankingV401,BerlinGroup,OBP-API,OBP-API-VRP"};
        }
        model.addAttribute("displayStandards", displayStandards);
        model.addAttribute("buttonBackgroundColor", buttonBackgroundColor);
        model.addAttribute("buttonHoverBackgroundColor", buttonHoverBackgroundColor);
        model.addAttribute("showBankLogo", showBankLogo);
        model.addAttribute("obpBaseUrl", obpBaseUrl);
        model.addAttribute("bankLogoUrl", bankLogoUrl);
        return "index";
    }

    private String returnErrorIfAny(@RequestHeader(required = false) String Referer) {
        if(Referer != null) {
            try {
                URL url = new URL(Referer);
                if(url.getQuery() != null && url.getQuery().contains("error=")) {
                    String query = url.getQuery();
                    String decodedQuery = Arrays.stream(query.split("&"))
                            .filter(param -> param.split("=")[0].startsWith("error"))
                            .map(param -> param.split("=")[0] + "=" + URLDecoder.decode(param.split("=")[1]))
                            .collect(Collectors.joining("<br>"));
                    String error = "<br>" + decodedQuery;
                    return error;
                }
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
        }
        return "";
    }

    @GetMapping({ "/index_uk", "index_uk.html"})
    public String index_uk(Model model) throws ParseException, JOSEException {
        {// initiate consent names
            String[] consents = ukPermissionScopes();
            model.addAttribute("consents", consents);
        }
        { // initiate all bank names and bank ids
            Banks banks = getBanks();
            model.addAttribute("banks", banks.getBanks());
            model.addAttribute("buttonBackgroundColor", buttonBackgroundColor);
            model.addAttribute("buttonHoverBackgroundColor", buttonHoverBackgroundColor);
            model.addAttribute("showBankLogo", showBankLogo);
            model.addAttribute("obpBaseUrl", obpBaseUrl);
            model.addAttribute("bankLogoUrl", bankLogoUrl);
        }
        return "index_uk";
    }

    @GetMapping({ "/index_uk4", "index_uk4.html"})
    public String index_uk4(Model model) throws ParseException, JOSEException {
        {// initiate consent names
            String[] consents = ukPermissionScopes();
            model.addAttribute("consents", consents);
        }
        { // initiate all bank names and bank ids
            Banks banks = getBanks();
            model.addAttribute("banks", banks.getBanks());
            model.addAttribute("buttonBackgroundColor", buttonBackgroundColor);
            model.addAttribute("buttonHoverBackgroundColor", buttonHoverBackgroundColor);
            model.addAttribute("showBankLogo", showBankLogo);
            model.addAttribute("obpBaseUrl", obpBaseUrl);
            model.addAttribute("bankLogoUrl", bankLogoUrl);
        }
        return "index_uk4";
    }



    /**
     * The UK permission codes among the configured scopes, for the consent checkboxes on the two UK
     * pages. Whatever the PSU ticks is sent verbatim as the consent's Permissions array, and the
     * ASPSP rejects an array holding anything that is not a UK permission code, so the OIDC scopes
     * configured alongside them (openid, offline, profile, email) must not be offered as choices.
     * Selecting by the UK "Read" prefix stays correct as codes are added, where excluding a fixed
     * list of non-permissions did not: every newly configured OIDC scope leaked into the form.
     */
    private String[] ukPermissionScopes() {
        return allScopes.stream()
                .filter(it -> it.startsWith("Read"))
                .filter(it -> !it.contains("BerlinGroup"))
                .toArray(String[]::new);
    }

    private Banks getBanks() {
        Banks banks = restTemplate.getForObject(getBanksUrl, Banks.class);
        return banks;
    }

    @GetMapping({"/index_bg", "index_bg.html"})
    public String index_bg(Model model, HttpSession session) {
        try {
            // Initiate consent names
            String[] consents = allScopes.stream()
                    .filter(it -> !"openid".equals(it) && !"offline".equals(it))
                    .filter(it -> it.contains("BerlinGroup"))
                    .toArray(String[]::new);
            model.addAttribute("consents", consents);

            // Initiate all bank names and bank ids
            Banks banks = getBanks();

            // Check if banks data is null (meaning the request failed or returned empty)
            if (banks == null || banks.getBanks().isEmpty()) {
                throw new RestClientException("Failed to retrieve bank data.");
            }

            // Add bank-related information to the model
            model.addAttribute("banks", banks.getBanks());
            model.addAttribute("buttonBackgroundColor", buttonBackgroundColor);
            model.addAttribute("buttonHoverBackgroundColor", buttonHoverBackgroundColor);
            model.addAttribute("showBankLogo", showBankLogo);
            model.addAttribute("obpBaseUrl", obpBaseUrl);
            model.addAttribute("bankLogoUrl", bankLogoUrl);
            redisService.readLogFromRedis(session, model);

            return "index_bg";

        } catch (HttpStatusCodeException e) {
            // Handle HTTP-specific errors from RestTemplate
            model.addAttribute("errorMsg", "External service error: " + e.getStatusCode() + " - " + e.getResponseBodyAsString());
            return "error";
        } catch (RestClientException e) {
            // Handle other RestTemplate errors (e.g., connection failures)
            model.addAttribute("errorMsg", e.getMessage());
            return "index_bg";
        } catch (Exception e) {
            // Generic catch-all for other exceptions
            model.addAttribute("errorMsg", "An unexpected error occurred: " + e.getMessage());
            return "error";
        }
    }
    
    @GetMapping({"/index_obp", "index_obp.html"})
    public String index_obp(Model model, HttpSession session) throws ParseException, JOSEException {
        {// initiate consent names
            // exclude "openid" and "offline", they are used by hydra
            String[] consents = allScopes.stream()
                    .filter(it -> !"openid".equals(it) && !"offline".equals(it))
                    .filter(it -> it.contains("Obp"))
                    .toArray(String[]::new);
            model.addAttribute("consents", consents);
        }
        { // initiate all bank names and bank ids
            Banks banks = getBanks();
            model.addAttribute("banks", banks.getBanks());
            model.addAttribute("buttonBackgroundColor", buttonBackgroundColor);
            model.addAttribute("buttonHoverBackgroundColor", buttonHoverBackgroundColor);
            model.addAttribute("showBankLogo", showBankLogo);
            model.addAttribute("obpBaseUrl", obpBaseUrl);
            model.addAttribute("bankLogoUrl", bankLogoUrl);
        }
        return "index_obp";
    }
    @GetMapping({"/index_obp_vrp", "index_obp_vrp.html"})
    public String index_obp_vrp(Model model, HttpSession session) throws ParseException, JOSEException {
        {// initiate consent names
            // exclude "openid" and "offline", they are used by hydra
            String[] consents = allScopes.stream()
                    .filter(it -> !"openid".equals(it) && !"offline".equals(it))
                    .filter(it -> it.contains("Obp"))
                    .toArray(String[]::new);
            model.addAttribute("consents", consents);
        }
        { // initiate all bank names and bank ids
            Banks banks = getBanks();
            model.addAttribute("banks", banks.getBanks());
            model.addAttribute("buttonBackgroundColor", buttonBackgroundColor);
            model.addAttribute("buttonHoverBackgroundColor", buttonHoverBackgroundColor);
            model.addAttribute("showBankLogo", showBankLogo);
            model.addAttribute("obpBaseUrl", obpBaseUrl);
            model.addAttribute("bankLogoUrl", bankLogoUrl);
        }
        return "index_obp_vrp";
    }
    @GetMapping({"/consents", "consents.html"})
    public String consents(Model model, HttpSession session) throws ParseException, JOSEException {
        { // initiate all bank names and bank ids
            Banks banks = getBanks();
            model.addAttribute("banks", banks.getBanks());
            model.addAttribute("buttonBackgroundColor", buttonBackgroundColor);
            model.addAttribute("buttonHoverBackgroundColor", buttonHoverBackgroundColor);
            model.addAttribute("showBankLogo", showBankLogo);
            model.addAttribute("obpBaseUrl", obpBaseUrl);
            model.addAttribute("bankLogoUrl", bankLogoUrl);
        }
        return "consents";
    }
    

    @PostMapping(value="/request_consents", params = {"bank", "consents", "transaction_from_time", "transaction_to_time", "expiration_time"})
    public String requestConsents(@RequestParam("bank") String bankId,
                                  @RequestParam String[] consents,
                                  @RequestParam String transaction_from_time,
                                  @RequestParam String transaction_to_time,
                                  @RequestParam String expiration_time,
                                  HttpSession session, Model model
                                  ) throws UnsupportedEncodingException, ParseException, JOSEException {
        try {
            final String consentId;
            {   // Create Account Access Consents
                String clientCredentialsToken = getClientCredentialsToken();
                HttpHeaders headers = new HttpHeaders();
                headers.setBearerAuth(clientCredentialsToken);

                // TODO it should have relation with rememberMe time
                String expirationDateTime = convertTimeFormat(expiration_time);
                ConsentPostBodyV310 body = new ConsentPostBodyV310(
                        bankId,
                        consents,
                        convertTimeFormat(transaction_from_time),
                        convertTimeFormat(transaction_to_time),
                        expirationDateTime);

                HttpEntity<ConsentPostBodyV310> request = new HttpEntity<>(body, headers);

                Map response = restTemplate.postForObject(createConsentsUrl, request, Map.class);
                consentId = ((Map<String, String>) response.get("Data")).get("ConsentId");
            }
            //{"client_id", "bank_id", "consent_id", "response_type=code", "scope", "redirect_uri", "state"})
            Map<String, String> queryParam = new LinkedHashMap<>();
            queryParam.put("client_id", clientId);
            queryParam.put("response_type", "code+id_token");
            // include OBP scopes, add OAuth2 and OIDC related scope: "openid" and "offline"
            consents = ArrayUtils.addAll(new String[]{"openid", "offline"}, consents);
            String scope = Stream.of(consents)
                    .distinct()
                    .map(this::encodeQueryParam)
                    .collect(Collectors.joining("+"));

            queryParam.put("scope", scope);
            String encodeRedirectUri = URLEncoder.encode(redirectUri, "UTF-8");
            queryParam.put("redirect_uri", encodeRedirectUri);
            final String state = UUID.randomUUID().toString();
            final String nonce = UUID.randomUUID().toString();
            queryParam.put("state", state);
            queryParam.put("nonce", nonce);
            SessionData.setState(session, state);
            SessionData.setNonce(session, nonce);

            // the parameter consent_id and bank_id are mandatory, these two parameter is not standard parameter of OAuth2 and OIDC
            queryParam.put("consent_id", consentId);
            queryParam.put("bank_id", bankId);
            queryParam.put("api_standard", "UKOpenBanking");
            SessionData.setApiStandard(session, "UKOpenBanking");
            // The consent is the credential the data calls will present, so hold on to it now
            // rather than waiting to read it back off the id_token in the callback.
            SessionData.setConsentId(session, consentId);
            // TODO the acr_values is just temp example value, can be space split values, need check and supply real values.
            //queryParam.put("acr_values", "urn:openbankproject:psd2:sca");

            // add code_challenge
            final String codeVerifier = PKCEUtil.generateCodeVerifier();
            SessionData.setCodeVerifier(session, codeVerifier);
            final String codeChallenge = PKCEUtil.generateCodeChallenge(codeVerifier);
            queryParam.put("code_challenge_method", "S256");
            queryParam.put("code_challenge", codeChallenge);

            String queryParamStr = queryParam.entrySet().stream().map(it -> it.getKey() + "=" + it.getValue()).collect(Collectors.joining("&"));
            String authorizationEndpoint = openIDConfiguration.getAuthorizationEndpoint();
            String redirectUrl = "redirect:" + authorizationEndpoint + "?" + queryParamStr;

            // if current user is authenticated, remove user info from session, to do re-authentication
            SessionData.remoteUserInfo(session);

            return redirectUrl;
        } catch (HttpStatusCodeException httpException) {
            logger.error("Error: ", httpException);
            String errorDetail = httpException.getStatusCode() + " " + httpException.getStatusText();
            String responseBody = httpException.getResponseBodyAsString();
            if (StringUtils.isNotBlank(responseBody)) {
                errorDetail += " - " + responseBody;
            }
            model.addAttribute("errorMsg", errorDetail);
            return "error";
        } catch (Exception unhandledException) {
            logger.error("Error: ", unhandledException);
            if(showUnhandledErrors) model.addAttribute("errorMsg", unhandledException);
            else model.addAttribute("errorMsg", "Internal Server Error");
            return "error";
        }
    }

    @PostMapping(value="/request_consents_uk4", params = {"bank", "consents", "transaction_from_time", "transaction_to_time", "expiration_time"})
    public String requestConsentsV401(@RequestParam("bank") String bankId,
                                  @RequestParam String[] consents,
                                  @RequestParam String transaction_from_time,
                                  @RequestParam String transaction_to_time,
                                  @RequestParam String expiration_time,
                                  HttpSession session, Model model
                                  ) throws UnsupportedEncodingException, ParseException, JOSEException {
        try {
            final String consentId;
            // Create Account Access Consents (v4.0.1). The server validates Permissions against the
            // UK combination rules and answers 400 for a set it must reject, so a checkbox selection
            // the profile forbids surfaces here as an HttpStatusCodeException carrying that reason.
            {
                String clientCredentialsToken = getClientCredentialsToken();
                HttpHeaders headers = new HttpHeaders();
                headers.setBearerAuth(clientCredentialsToken);

                // TODO it should have relation with rememberMe time
                String expirationDateTime = convertTimeFormat(expiration_time);
                ConsentPostBodyV310 body = new ConsentPostBodyV310(
                        bankId,
                        consents,
                        convertTimeFormat(transaction_from_time),
                        convertTimeFormat(transaction_to_time),
                        expirationDateTime);

                HttpEntity<ConsentPostBodyV310> request = new HttpEntity<>(body, headers);

                Map response = restTemplate.postForObject(createConsentsUrlV401, request, Map.class);
                consentId = ((Map<String, String>) response.get("Data")).get("ConsentId");
            }
            //{"client_id", "bank_id", "consent_id", "response_type=code", "scope", "redirect_uri", "state"})
            Map<String, String> queryParam = new LinkedHashMap<>();
            queryParam.put("client_id", clientId);
            queryParam.put("response_type", "code+id_token");
            // include OBP scopes, add OAuth2 and OIDC related scope: "openid" and "offline"
            consents = ArrayUtils.addAll(new String[]{"openid", "offline"}, consents);
            String scope = Stream.of(consents)
                    .distinct()
                    .map(this::encodeQueryParam)
                    .collect(Collectors.joining("+"));

            queryParam.put("scope", scope);
            String encodeRedirectUri = URLEncoder.encode(redirectUri, "UTF-8");
            queryParam.put("redirect_uri", encodeRedirectUri);
            final String state = UUID.randomUUID().toString();
            final String nonce = UUID.randomUUID().toString();
            queryParam.put("state", state);
            queryParam.put("nonce", nonce);
            SessionData.setState(session, state);
            SessionData.setNonce(session, nonce);

            // the parameter consent_id and bank_id are mandatory, these two parameter is not standard parameter of OAuth2 and OIDC
            queryParam.put("consent_id", consentId);
            queryParam.put("bank_id", bankId);
            queryParam.put("api_standard", "UKOpenBankingV401");
            SessionData.setApiStandard(session, "UKOpenBankingV401");
            SessionData.setBankId(session, bankId);
            // The consent is the credential the data calls will present, so hold on to it now
            // rather than waiting to read it back off the id_token in the callback.
            SessionData.setConsentId(session, consentId);
            // TODO the acr_values is just temp example value, can be space split values, need check and supply real values.
            //queryParam.put("acr_values", "urn:openbankproject:psd2:sca");

            // add code_challenge
            final String codeVerifier = PKCEUtil.generateCodeVerifier();
            SessionData.setCodeVerifier(session, codeVerifier);
            final String codeChallenge = PKCEUtil.generateCodeChallenge(codeVerifier);
            queryParam.put("code_challenge_method", "S256");
            queryParam.put("code_challenge", codeChallenge);

            String queryParamStr = queryParam.entrySet().stream().map(it -> it.getKey() + "=" + it.getValue()).collect(Collectors.joining("&"));
            String authorizationEndpoint = openIDConfiguration.getAuthorizationEndpoint();
            String redirectUrl = "redirect:" + authorizationEndpoint + "?" + queryParamStr;

            // if current user is authenticated, remove user info from session, to do re-authentication
            SessionData.remoteUserInfo(session);

            return redirectUrl;
        } catch (HttpStatusCodeException httpException) {
            logger.error("Error: ", httpException);
            String errorDetail = httpException.getStatusCode() + " " + httpException.getStatusText();
            String responseBody = httpException.getResponseBodyAsString();
            if (StringUtils.isNotBlank(responseBody)) {
                errorDetail += " - " + responseBody;
            }
            model.addAttribute("errorMsg", errorDetail);
            return "error";
        } catch (Exception unhandledException) {
            logger.error("Error: ", unhandledException);
            if(showUnhandledErrors) model.addAttribute("errorMsg", unhandledException);
            else model.addAttribute("errorMsg", "Internal Server Error");
            return "error";
        }
    }

    @GetMapping(value={"/main", "main.html"}, params={"code", "id_token", "state"})
    public String main(@RequestParam("code") String code,
                       @RequestParam("id_token") String idToken,
                       @RequestParam(value = "access_token", required = false) String accessToken,
                       @RequestParam("state") String state,
                       Model model,
                       HttpSession session) throws ParseException, JOSEException, NoSuchAlgorithmException {
        // when repeat call with same code, just do nothing.
        if(code.equals(SessionData.getCode(session))) {
            return "accounts";
        }
        if(!state.equals(SessionData.getState(session))) {
            model.addAttribute("errorMsg", "The request parameter [state] is not correct!");
            return "error";
        }
        { // validate c_hash, at_hash and s_hash
            final JWT idTokenJwt = JWTParser.parse(idToken);
            final String alg = idTokenJwt.getHeader().getAlgorithm().getName().replaceFirst(".*?S(\\d+)$", "SHA-$1");
            final JWTClaimsSet idTokenJwtJWTClaims = idTokenJwt.getJWTClaimsSet();

            final Object cHash = idTokenJwtJWTClaims.getClaim("c_hash");
            if(!buildHash(code, alg).equals(cHash)) {
                model.addAttribute("errorMsg", "The c_hash is not correct in id_token");
                return "error";
            }
            final Object sHash = idTokenJwtJWTClaims.getClaim("s_hash");
            if(!buildHash(state, alg).equals(sHash)) {
                model.addAttribute("errorMsg", "The s_hash is not correct in id_token");
                return "error";
            }
            final Object atHash = idTokenJwtJWTClaims.getClaim("at_hash");
            if(accessToken != null && !buildHash(accessToken, alg).equals(atHash)) {
                model.addAttribute("errorMsg", "The at_hash is not correct in id_token");
                return "error";
            }
            String consentId = (String) idTokenJwtJWTClaims.getClaim("consent_id");
            logger.debug("consentId: " + consentId);
            SessionData.setConsentId(session, consentId);
        }
        SessionData.setCode(session, code);

        // The UK standards stop here. The PSU has just authorised the consent, which is all these
        // flows need: the data calls present that consent and nothing else, so there is no reason
        // to exchange the code for an access token we would never send. Same shape as Berlin Group,
        // which returns from its SCA redirect without any token round-trip at all.
        //
        // The redirect through OIDC is still required -- it is what carries the PSU through the
        // authorise ceremony that binds the consent to them and grants the consented views. Only
        // the token exchange is dropped.
        String apiStandard = SessionData.getApiStandard(session);
        if ("UKOpenBanking".equals(apiStandard) || "UKOpenBankingV401".equals(apiStandard)) {
            return "redirect:/main";
        }

        // get tokens use code
        {
            HttpHeaders headers = new HttpHeaders();
            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("grant_type", "authorization_code");
            body.add("code", code);
            body.add("redirect_uri", redirectUri);
            body.add("client_id", clientId);
            // ADD code_verifier
            final String codeVerifier = SessionData.getCodeVerifier(session);
            body.add("code_verifier", codeVerifier);

            body.add("client_secret", clientSecret);

            HttpEntity<MultiValueMap> request = new HttpEntity<>(body, headers);
            String tokenEndpoint = openIDConfiguration.getTokenEndpoint();
            TokenResponse tokenResponse = restTemplate.postForObject(tokenEndpoint, request, TokenResponse.class);

            SessionData.setIdToken(session, tokenResponse.getId_token());
            SessionData.setAccessToken(session, tokenResponse.getAccess_token());
            SessionData.setRefreshToken(session, tokenResponse.getRefresh_token());

            logger.debug("idToken:\n" + tokenResponse.getId_token());
            logger.debug("accessToken:\n" + tokenResponse.getAccess_token());
        }

        { // fetch user information
            try {
                HttpHeaders headers = new HttpHeaders();
                headers.setBearerAuth(SessionData.getAccessToken(session));
                HttpEntity<String> entity = new HttpEntity<>(headers);
                ResponseEntity<UserInfo> userInfoResponse = restTemplate.exchange(currentUserUrl, HttpMethod.GET, entity, UserInfo.class);
                SessionData.setUserInfo(session, userInfoResponse.getBody());
                logger.debug("login success user:" + userInfoResponse.getBody().getUsername());
            } catch (HttpClientErrorException e) {
                String error = "Sorry! Cannot create the consent.";
                logger.error(error, e);
                model.addAttribute("errorMsg", e.getMessage());
                return "error";
            }
        }
        return "redirect:/main";
    }

    // Consent flow callback — OBP-OIDC redirects here with consent_id after Portal consent approval.
    // No OAuth code/token exchange needed — Hola uses Consent-Id + Consumer-Key headers to access OBP-API.
    @GetMapping(value={"/main", "main.html"}, params={"consent_status", "consent_id"})
    public String mainConsentCallback(@RequestParam("consent_id") String consentId,
                                      @RequestParam("consent_status") String consentStatus,
                                      @RequestParam(value = "state", required = false) String state,
                                      Model model,
                                      HttpSession session) {
        logger.info("Consent callback received: consent_id=" + consentId + ", consent_status=" + consentStatus);

        if (!"ACCEPTED".equals(consentStatus) && !"VALID".equals(consentStatus)) {
            model.addAttribute("errorMsg", "Consent was not approved. Status: " + consentStatus);
            return "error";
        }

        if (state != null && !state.equals(SessionData.getState(session))) {
            model.addAttribute("errorMsg", "The request parameter [state] is not correct!");
            return "error";
        }

        SessionData.setConsentId(session, consentId);
        logger.info("Consent-Id stored in session: " + consentId);
        logger.info("Session apiStandard: " + SessionData.getApiStandard(session));
        logger.info("Session bankId: " + SessionData.getBankId(session));

        return "redirect:/main";
    }

    @Autowired
    private RedisService redisService;

    @GetMapping(value={"/main", "main.html"}, params="!code")
    public String main(HttpSession session, Model model) {
        redisService.readLogFromRedis(session, model);
        String apiStandard = SessionData.getApiStandard(session);
        model.addAttribute("apiStandard", apiStandard);
        UserInfo user = SessionData.getUserInfo(session);
        model.addAttribute("user", user);
        String consentId = SessionData.getConsentId(session);
        model.addAttribute("consentId", consentId);
        String consentRequestId = SessionData.getConsentRequestId(session);
        model.addAttribute("consentRequestId", consentRequestId);
        // Berlin Group has no OAuth token to hang this off (see requestConsentsBerlinGroup), so unlike
        // the other flows this can't be captured once at OIDC-callback time — fetch it fresh here,
        // authenticating with the Consent-ID header the same way getAccountsBerlinGroup does.
        if ("BerlinGroup".equalsIgnoreCase(apiStandard) && StringUtils.isNotBlank(consentId)) {
            try {
                HttpHeaders consentInfoHeaders = new HttpHeaders();
                consentInfoHeaders.add("Consent-ID", consentId);
                HttpEntity<String> consentInfoEntity = new HttpEntity<>(consentInfoHeaders);
                ResponseEntity<Map> consentInfoResponse = restTemplate.exchange(
                        getConsentInformationBerlinGroup.replace("CONSENT_ID", consentId),
                        HttpMethod.GET, consentInfoEntity, Map.class);
                Map consentInfoBody = consentInfoResponse.getBody();
                session.setAttribute("frequencyPerDay", String.valueOf(consentInfoBody.get("frequencyPerDay")));
                session.setAttribute("consentStatus", String.valueOf(consentInfoBody.get("consentStatus")));
                session.setAttribute("validUntil", String.valueOf(consentInfoBody.get("validUntil")));
                session.setAttribute("recurringIndicator", String.valueOf(consentInfoBody.get("recurringIndicator")));
            } catch (RestClientException e) {
                logger.warn("Could not fetch Berlin Group consent info for consent_id=" + consentId, e);
            }
        }
        // UK v4.0.1: show the consent that governs the data calls -- the same one they present, read
        // back from OBP-API so the status and permissions shown are the live server-side values
        // rather than anything this app remembers.
        if ("UKOpenBankingV401".equals(apiStandard)) {
            String ukConsentId = SessionData.getConsentId(session);
            model.addAttribute("ukConsentId", ukConsentId);
            if (StringUtils.isNotBlank(ukConsentId)) {
                try {
                    // Read through the consent itself, exactly as the data calls do. This endpoint
                    // enforces ownership -- only the PSU the consent is bound to may read it -- and
                    // the consent resolves to that PSU, so it passes.
                    HttpHeaders ukConsentHeaders = new HttpHeaders();
                    ukConsentHeaders.add("Consent-Id", ukConsentId);
                    ukConsentHeaders.add("Consumer-Key", clientId);
                    HttpEntity<String> ukConsentEntity = new HttpEntity<>(ukConsentHeaders);
                    ResponseEntity<Map> ukConsentResponse = restTemplate.exchange(
                            getConsentInformationUrlV401.replace("CONSENT_ID", ukConsentId),
                            HttpMethod.GET, ukConsentEntity, Map.class);
                    Map ukConsentData = (Map) ukConsentResponse.getBody().get("Data");
                    model.addAttribute("ukConsentStatus", String.valueOf(ukConsentData.get("Status")));
                    // The consent stores one view per (account, permission) pair, so a consent covering
                    // several accounts repeats each permission. De-duplicate for display -- what the PSU
                    // cares about is which permissions were granted, not how many accounts they span.
                    Object rawPermissions = ukConsentData.get("Permissions");
                    if (rawPermissions instanceof Collection) {
                        Set<String> permissions = ((Collection<?>) rawPermissions).stream()
                                .map(String::valueOf)
                                .collect(Collectors.toCollection(LinkedHashSet::new));
                        model.addAttribute("ukConsentPermissions", permissions);
                        // Machine-readable copy for the page script: GET /aisp/accounts answers 200
                        // with an empty list when the consent grants no ReadAccounts* view, so the
                        // script needs the granted permissions to explain an empty result rather
                        // than scraping the text it just rendered.
                        model.addAttribute("ukConsentPermissionsCsv", String.join(",", permissions));
                    } else {
                        model.addAttribute("ukConsentPermissions", rawPermissions);
                        model.addAttribute("ukConsentPermissionsCsv",
                                rawPermissions == null ? "" : String.valueOf(rawPermissions));
                    }
                    model.addAttribute("ukConsentExpiration", String.valueOf(ukConsentData.get("ExpirationDateTime")));
                } catch (RestClientException e) {
                    // Non-fatal: the page still works, it just can't show the consent's live status.
                    logger.warn("Could not fetch UK v4.0.1 consent info for consent_id=" + ukConsentId, e);
                    model.addAttribute("ukConsentError", e.getMessage());
                }
            }
        }
        String consentStatus = (String)session.getAttribute("consentStatus");
        model.addAttribute("consentStatus", consentStatus);
        String frequencyPerDay = (String)session.getAttribute("frequencyPerDay");
        model.addAttribute("frequencyPerDay", frequencyPerDay);
        String validUntil = (String)session.getAttribute("validUntil");
        model.addAttribute("validUntil", validUntil);
        String validFrom = (String)session.getAttribute("validFrom");
        model.addAttribute("validFrom", validFrom);
        String recurringIndicator = (String)session.getAttribute("recurringIndicator");
        model.addAttribute("recurringIndicator", recurringIndicator);
        model.addAttribute("showBankLogo", showBankLogo);
        model.addAttribute("obpBaseUrl", obpBaseUrl);
        model.addAttribute("bankLogoUrl", bankLogoUrl);
        return "main";
    }


    @PostMapping(value="/request_consents_bg", params = {"bank", "iban","consents", "recurring_indicator", "frequency_per_day", "expiration_time"})
    public String requestConsentsBerlinGroup(@RequestParam("bank") String bankId,
                                             @RequestParam("iban") String iban,
                                             @RequestParam String[] consents,
                                             @RequestParam String recurring_indicator,
                                             @RequestParam String frequency_per_day,
                                             @RequestParam String expiration_time,
                                             HttpSession session, Model model
    ) throws UnsupportedEncodingException, ParseException, JOSEException, RestClientException {
        try {
            // Create Berlin Group Consent
            String clientCredentialsToken = getClientCredentialsToken();
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(clientCredentialsToken);
            // OBP-API stores these into the consent's JWT (ConsentUtil.createObpConsentBerlinGroup)
            // and Portal's SCA confirmation flow reads TPP-Redirect-URI back out of it to send the
            // browser here once the consent is ACCEPTED, so main() (params="!code") can render it
            // straight out of session — no OAuth code/token round-trip needed for Berlin Group.
            headers.add("TPP-Redirect-URI", redirectUri);
            headers.add("TPP-Nok-Redirect-URI", redirectUri);
            String recurringIndicator = recurring_indicator;
            String expirationDateTime = convertTimeFormat(expiration_time);
            // Berlin Group's `validUntil` is validated server-side as a plain yyyy-MM-dd date,
            // not the full timestamp `expirationDateTime` carries for the other flows/the redirect below.
            String validUntilDate = expiration_time.substring(0, 10);
            String frequencyPerDay = frequency_per_day;
            String[] ibans = iban.split(",");
            for(int i=0; i< ibans.length; i++){
                ibans[i] = ibans[i].trim().replace(" ", "");
            }
            PostConsentJson body = new PostConsentJson(
                    consents,
                    ibans,
                    recurringIndicator.equalsIgnoreCase("true"),
                    validUntilDate,
                    Integer.parseInt(frequencyPerDay),
                    false
            );
            String consentId;
            String scaRedirectUrl;
            try {
                HttpEntity<PostConsentJson> request = new HttpEntity<>(body, headers);
                Map response = restTemplate.postForObject(createBerlinGroupConsentsUrl, request, Map.class);
                consentId = ((Map<String, String>) response).get("consentId");
                Map links = (Map) response.get("_links");
                Map scaRedirect = links == null ? null : (Map) links.get("scaRedirect");
                scaRedirectUrl = scaRedirect == null ? null : (String) scaRedirect.get("href");
                if (StringUtils.isBlank(scaRedirectUrl)) {
                    model.addAttribute("errorMsg", "OBP-API did not return a consent._links.scaRedirect URL "
                            + "to confirm this consent (response: " + response + "). Without it there is no way "
                            + "to complete Berlin Group's SCA step.");
                    return "error";
                }
                // main() (params="!code") reads all of this straight out of session when the browser
                // lands back on redirectUri after SCA confirmation on Portal — Berlin Group's consent
                // is authorized via Consent-ID, not an OAuth token, so there's no code/id_token exchange
                // to do here the way the other flows need.
                SessionData.setConsentId(session, consentId);
                SessionData.setApiStandard(session, "BerlinGroup");
                SessionData.setBankId(session, bankId);
            } catch (HttpClientErrorException e) {
                String error = "Sorry! Cannot create the consent.";
                logger.error(error, e);
                model.addAttribute("errorMsg", e.getMessage());
                return "error";
            } catch (RestClientException e) {
                // Handle other RestTemplate errors (e.g., connection failures)
                model.addAttribute("errorMsg", e.getMessage());
                return "index_bg";
            }

            // if current user is authenticated, remove user info from session, to do re-authentication
            SessionData.remoteUserInfo(session);

            return "redirect:" + scaRedirectUrl;
        } catch (HttpStatusCodeException httpException) {
            logger.error("Error: ", httpException);
            String errorDetail = httpException.getStatusCode() + " " + httpException.getStatusText();
            String responseBody = httpException.getResponseBodyAsString();
            if (StringUtils.isNotBlank(responseBody)) {
                errorDetail += " - " + responseBody;
            }
            model.addAttribute("errorMsg", errorDetail);
            return "error";
        } catch (Exception unhandledException) {
            logger.error("Error: ", unhandledException);
            if(showUnhandledErrors) model.addAttribute("errorMsg", unhandledException);
            else model.addAttribute("errorMsg", "Internal Server Error");
            return "error";
        }
    }
    @PostMapping(value="/request_consents_obp", params = {"bank", "time_to_live_in_seconds", "valid_from", "everything_indicator", "permission_routing_scheme", "permission_routing_address", "permission_view_id", "permission_routing_scheme_2", "permission_routing_address_2", "permission_view_id_2"})
    public String requestConsentsOpenBankProject(@RequestParam("bank") String bankId,
                                                 @RequestParam("time_to_live_in_seconds") String timeToLiveInSeconds,
                                                 @RequestParam("valid_from") String validFrom,
                                                 @RequestParam("everything_indicator") String everythingIndicator,
                                                 @RequestParam("permission_routing_scheme") String permissionRoutingScheme,
                                                 @RequestParam("permission_routing_address") String permissionRoutingAddress,
                                                 @RequestParam("permission_view_id") String permissionViewId,
                                                 @RequestParam("permission_routing_scheme_2") String permissionRoutingScheme2,
                                                 @RequestParam("permission_routing_address_2") String permissionRoutingAddress2,
                                                 @RequestParam("permission_view_id_2") String permissionViewId2,
                                                 HttpSession session, Model model
    ) throws UnsupportedEncodingException, ParseException, JOSEException, RestClientException {
        try {
            // Create OBP Consent
            String clientCredentialsToken = getClientCredentialsToken();
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(clientCredentialsToken);
            String validFromTime = localToGMT(validFrom);
            
            boolean everything = everythingIndicator.equalsIgnoreCase("true");

            AccountRouting accountRoutingRow1 = new AccountRouting(permissionRoutingScheme, permissionRoutingAddress);
            AccountAccess accountAccessRow1 = new AccountAccess(accountRoutingRow1, permissionViewId);
            AccountRouting accountRoutingRow2 = new AccountRouting(permissionRoutingScheme2, permissionRoutingAddress2);
            AccountAccess accountAccessRow2 = new AccountAccess(accountRoutingRow2, permissionViewId2);
            List<AccountAccess> accountAccessArrayList = new ArrayList<>();
            if(!everything) {
                if(!permissionRoutingScheme.isEmpty() && !permissionRoutingAddress.isEmpty() && !permissionViewId.isEmpty()) {
                    accountAccessArrayList.add(accountAccessRow1);
                }
                if(!permissionRoutingScheme2.isEmpty() && !permissionRoutingAddress2.isEmpty() && !permissionViewId2.isEmpty()) {
                    accountAccessArrayList.add(accountAccessRow2);
                }
            }
            
            PostConsentRequestJson body = new PostConsentRequestJson(
                    everything,
                    bankId,
                    Integer.parseInt(timeToLiveInSeconds),
                    validFromTime,
                    accountAccessArrayList
            );
            String consentRequestId = "";
            try {
                HttpEntity<PostConsentRequestJson> request = new HttpEntity<>(body, headers);
                Map response = restTemplate.postForObject(createConsentRequest, request, Map.class);
                consentRequestId = ((Map<String, String>) response).get("consent_request_id");
                session.setAttribute("consent_request_id", consentRequestId);
                session.setAttribute("consent_id", "None");
            } catch (HttpClientErrorException e) {
                String error = "Sorry! Cannot create the consent.";
                logger.error(error, e);
                model.addAttribute("errorMsg", e.getMessage());
                return "error";
            }


            //{"client_id", "bank_id", "consent_id", "response_type=code", "scope", "redirect_uri", "state"})
            Map<String, String> queryParam = new LinkedHashMap<>();
            queryParam.put("client_id", clientId);
            queryParam.put("response_type", "code+id_token");
            // include OBP scopes, add OAuth2 and OIDC related scope: "openid" and "offline"
            String scope = Stream.of(new String[]{"openid", "offline"})
                    .distinct()
                    .map(this::encodeQueryParam)
                    .collect(Collectors.joining("+"));

            queryParam.put("scope", scope);
            String encodeRedirectUri = URLEncoder.encode(redirectUri, "UTF-8");
            queryParam.put("redirect_uri", encodeRedirectUri);
            final String state = UUID.randomUUID().toString();
            final String nonce = UUID.randomUUID().toString();
            queryParam.put("state", state);
            queryParam.put("nonce", nonce);
            SessionData.setState(session, state);
            SessionData.setNonce(session, nonce);

            // the parameter consent_id and bank_id are mandatory, these two parameter is not standard parameter of OAuth2 and OIDC
            queryParam.put("consent_request_id", consentRequestId);
            queryParam.put("consent_id", "None");
            queryParam.put("bank_id", bankId);
            queryParam.put("time_to_live_in_seconds", timeToLiveInSeconds);
            queryParam.put("valid_from", validFromTime);
            queryParam.put("api_standard", "OBP");
            queryParam.put("everything_indicator", everythingIndicator);
            SessionData.setApiStandard(session, "OBP");
            SessionData.setBankId(session, bankId);
            // TODO the acr_values is just temp example value, can be space split values, need check and supply real values.
            //queryParam.put("acr_values", "urn:openbankproject:psd2:sca");

            // add code_challenge
            final String codeVerifier = PKCEUtil.generateCodeVerifier();
            SessionData.setCodeVerifier(session, codeVerifier);
            final String codeChallenge = PKCEUtil.generateCodeChallenge(codeVerifier);
            queryParam.put("code_challenge_method", "S256");
            queryParam.put("code_challenge", codeChallenge);

            String queryParamStr = queryParam.entrySet().stream().map(it -> it.getKey() + "=" + it.getValue()).collect(Collectors.joining("&"));
            String authorizationEndpoint = openIDConfiguration.getAuthorizationEndpoint();
            String redirectUrl = "redirect:" + authorizationEndpoint + "?" + queryParamStr;

            // if current user is authenticated, remove user info from session, to do re-authentication
            SessionData.remoteUserInfo(session);

            return redirectUrl;
        } catch (HttpStatusCodeException httpException) {
            logger.error("Error: ", httpException);
            String errorDetail = httpException.getStatusCode() + " " + httpException.getStatusText();
            String responseBody = httpException.getResponseBodyAsString();
            if (StringUtils.isNotBlank(responseBody)) {
                errorDetail += " - " + responseBody;
            }
            model.addAttribute("errorMsg", errorDetail);
            return "error";
        } catch (Exception unhandledException) {
            logger.error("Error: ", unhandledException);
            if(showUnhandledErrors) model.addAttribute("errorMsg", unhandledException);
            else model.addAttribute("errorMsg", "Internal Server Error");
            return "error";
        }
    }
    @PostMapping(value="/request_consents_obp_vrp", params = {"bank", 
            "time_to_live_in_seconds", "valid_from", "email", "phone_number", 
            "from_bank_routing_scheme", "from_bank_routing_address", 
            "from_routing_scheme", "from_routing_address",
            "to_bank_routing_scheme", "to_bank_routing_address",
            "to_branch_routing_scheme", "to_branch_routing_address",
            "to_routing_scheme", "to_routing_address", 
            "currency", "max_single_amount", "counterparty_name",
            "max_monthly_amount", "max_yearly_amount", "max_number_of_monthly_transactions", "max_number_of_yearly_transactions",
            "max_total_amount", "max_number_of_transactions"})
    public String requestConsentsVrpOpenBankProject(@RequestParam("bank") String bankId, 
                                                    @RequestParam("time_to_live_in_seconds") String timeToLiveInSeconds,
                                                    @RequestParam("valid_from") String validFrom,
                                                    @RequestParam("email") String email, 
                                                    @RequestParam("phone_number") String phoneNumber,
                                                    @RequestParam("from_bank_routing_scheme") String fromBankRoutingScheme,
                                                    @RequestParam("from_bank_routing_address") String fromBankRoutingAddress,
                                                    @RequestParam("from_routing_scheme") String fromRoutingScheme,
                                                    @RequestParam("from_routing_address") String fromRoutingAddress,
                                                    @RequestParam("to_bank_routing_scheme") String toBankRoutingScheme,
                                                    @RequestParam("to_bank_routing_address") String toBankRoutingAddress,
                                                    @RequestParam("to_branch_routing_scheme") String toBranchRoutingScheme,
                                                    @RequestParam("to_branch_routing_address") String toBranchRoutingAddress,
                                                    @RequestParam("to_routing_scheme") String toRoutingScheme,
                                                    @RequestParam("to_routing_address") String toRoutingAddress,
                                                    @RequestParam("counterparty_name") String counterpartyName,
                                                    @RequestParam("currency") String currency,
                                                    @RequestParam("max_single_amount") String maxSingleAmount,
                                                    @RequestParam("max_monthly_amount") String maxMonthlyAmount,
                                                    @RequestParam("max_yearly_amount") String maxYearlyAmount,
                                                    @RequestParam("max_number_of_monthly_transactions") String maxNumberOfMonthlyTransactions,
                                                    @RequestParam("max_number_of_yearly_transactions") String maxNumberOfYearlyTransactions,
                                                    @RequestParam("max_total_amount") String maxTotalAmount,
                                                    @RequestParam("max_number_of_transactions") String maxNumberOfTransactions,
                                                 HttpSession session, Model model
    ) throws UnsupportedEncodingException, ParseException, JOSEException, RestClientException {
        try {
            // Create OBP Consent
            String clientCredentialsToken = getClientCredentialsToken();
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(clientCredentialsToken);
            String validFromTime = localToGMT(validFrom);

            PostConsentRequestVrpJson body = new PostConsentRequestVrpJson(
                    new FromAccount(
                            new BankRouting(fromBankRoutingScheme, fromBankRoutingAddress),
                            new BranchRouting("", ""),
                            new AccountRouting(fromRoutingScheme, fromRoutingAddress)
                    ),
                    new ToAccount(
                            // The PSU's own label for the payee. It is bound from the form above and
                            // was being dropped here, so the approval screen had no name to show for
                            // who may be paid under this mandate, and the counterparty OBP creates
                            // from it was left unnamed too.
                            counterpartyName,
                            new BankRouting(toBankRoutingScheme, toBankRoutingAddress),
                            new BranchRouting(toBranchRoutingScheme, toBranchRoutingAddress),
                            new AccountRouting(toRoutingScheme, toRoutingAddress),
                            // Argument order follows OBP-API's PostCounterpartyLimitV510: the
                            // amounts are Strings and each is followed by its own transaction
                            // count. Passing them in form order instead silently swapped
                            // max_yearly_amount with max_number_of_monthly_transactions.
                            new Limit(
                                    currency,
                                    maxSingleAmount,
                                    maxMonthlyAmount,
                                    Integer.parseInt(maxNumberOfMonthlyTransactions),
                                    maxYearlyAmount,
                                    Integer.parseInt(maxNumberOfYearlyTransactions),
                                    maxTotalAmount,
                                    Integer.parseInt(maxNumberOfTransactions)
                            )
                    ),
                    Integer.parseInt(timeToLiveInSeconds),
                    validFromTime,
                    email,
                    phoneNumber
            );
            String consentRequestId = "";
            try {
                HttpEntity<PostConsentRequestVrpJson> request = new HttpEntity<>(body, headers);
                Map response = restTemplate.postForObject(createConsentRequestVrp, request, Map.class);
                consentRequestId = ((Map<String, String>) response).get("consent_request_id");
                session.setAttribute("consent_request_id", consentRequestId);
                session.setAttribute("consent_id", "None");
            } catch (HttpClientErrorException e) {
                String error = "Sorry! Cannot create the consent.";
                logger.error(error, e);
                model.addAttribute("errorMsg", e.getMessage());
                return "error";
            }


            //{"client_id", "bank_id", "consent_id", "response_type=code", "scope", "redirect_uri", "state"})
            Map<String, String> queryParam = new LinkedHashMap<>();
            queryParam.put("client_id", clientId);
            queryParam.put("response_type", "code+id_token");
            // include OBP scopes, add OAuth2 and OIDC related scope: "openid" and "offline"
            String scope = Stream.of(new String[]{"openid", "offline"})
                    .distinct()
                    .map(this::encodeQueryParam)
                    .collect(Collectors.joining("+"));

            queryParam.put("scope", scope);
            String encodeRedirectUri = URLEncoder.encode(redirectUri, "UTF-8");
            queryParam.put("redirect_uri", encodeRedirectUri);
            final String state = UUID.randomUUID().toString();
            final String nonce = UUID.randomUUID().toString();
            queryParam.put("state", state);
            queryParam.put("nonce", nonce);
            SessionData.setState(session, state);
            SessionData.setNonce(session, nonce);

            // the parameter consent_id and bank_id are mandatory, these two parameter is not standard parameter of OAuth2 and OIDC
            queryParam.put("consent_request_id", consentRequestId);
            queryParam.put("consent_id", "None");
            queryParam.put("bank_id", bankId);
            queryParam.put("time_to_live_in_seconds", timeToLiveInSeconds);
            queryParam.put("valid_from", validFromTime);
            queryParam.put("api_standard", "OBP");
            SessionData.setApiStandard(session, "OBP");
            SessionData.setBankId(session, bankId);
            // TODO the acr_values is just temp example value, can be space split values, need check and supply real values.
            //queryParam.put("acr_values", "urn:openbankproject:psd2:sca");

            // add code_challenge
            final String codeVerifier = PKCEUtil.generateCodeVerifier();
            SessionData.setCodeVerifier(session, codeVerifier);
            final String codeChallenge = PKCEUtil.generateCodeChallenge(codeVerifier);
            queryParam.put("code_challenge_method", "S256");
            queryParam.put("code_challenge", codeChallenge);

            String queryParamStr = queryParam.entrySet().stream().map(it -> it.getKey() + "=" + it.getValue()).collect(Collectors.joining("&"));
            String authorizationEndpoint = openIDConfiguration.getAuthorizationEndpoint();
            String redirectUrl = "redirect:" + authorizationEndpoint + "?" + queryParamStr;

            // if current user is authenticated, remove user info from session, to do re-authentication
            SessionData.remoteUserInfo(session);

            return redirectUrl;
        } catch (HttpStatusCodeException httpException) {
            logger.error("Error: ", httpException);
            String errorDetail = httpException.getStatusCode() + " " + httpException.getStatusText();
            String responseBody = httpException.getResponseBodyAsString();
            if (StringUtils.isNotBlank(responseBody)) {
                errorDetail += " - " + responseBody;
            }
            model.addAttribute("errorMsg", errorDetail);
            return "error";
        } catch (Exception unhandledException) {
            logger.error("Error: ", unhandledException);
            if(showUnhandledErrors) model.addAttribute("errorMsg", unhandledException);
            else model.addAttribute("errorMsg", "Internal Server Error");
            return "error";
        }
    }
    @PostMapping(value="/administrate_consents", params = {})
    public String administrateConsents(@RequestParam("bank") String bankId, HttpSession session, Model model
    ) throws UnsupportedEncodingException, ParseException, JOSEException, RestClientException {
        try {
            String clientCredentialsToken = getClientCredentialsToken();
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(clientCredentialsToken);
            

            //{"client_id", "bank_id", "consent_id", "response_type=code", "scope", "redirect_uri", "state"})
            Map<String, String> queryParam = new LinkedHashMap<>();
            queryParam.put("client_id", clientId);
            queryParam.put("response_type", "code+id_token");
            // include OBP scopes, add OAuth2 and OIDC related scope: "openid" and "offline"
            String scope = Stream.of(ArrayUtils.addAll(new String[]{"openid", "offline"}))
                    .distinct()
                    .map(this::encodeQueryParam)
                    .collect(Collectors.joining("+"));

            queryParam.put("scope", scope);
            
            
            String encodeRedirectUri = URLEncoder.encode(redirectUri, "UTF-8");
            queryParam.put("redirect_uri", encodeRedirectUri);
            final String state = UUID.randomUUID().toString();
            final String nonce = UUID.randomUUID().toString();
            queryParam.put("state", state);
            queryParam.put("nonce", nonce);
            SessionData.setState(session, state);
            SessionData.setNonce(session, nonce);

            // the parameter consent_id and bank_id are mandatory, these two parameter is not standard parameter of OAuth2 and OIDC
            queryParam.put("bank_id", bankId);
            queryParam.put("consent_id", "Utility-List-Consents");
            queryParam.put("api_standard", "BerlinGroup");
            SessionData.setApiStandard(session, "BerlinGroup");

            // add code_challenge
            final String codeVerifier = PKCEUtil.generateCodeVerifier();
            SessionData.setCodeVerifier(session, codeVerifier);
            final String codeChallenge = PKCEUtil.generateCodeChallenge(codeVerifier);
            queryParam.put("code_challenge_method", "S256");
            queryParam.put("code_challenge", codeChallenge);

            String queryParamStr = queryParam.entrySet().stream().map(it -> it.getKey() + "=" + it.getValue()).collect(Collectors.joining("&"));
            String authorizationEndpoint = openIDConfiguration.getAuthorizationEndpoint();
            String redirectUrl = "redirect:" + authorizationEndpoint + "?" + queryParamStr;

            // if current user is authenticated, remove user info from session, to do re-authentication
            SessionData.remoteUserInfo(session);

            return redirectUrl;
        } catch (HttpStatusCodeException httpException) {
            logger.error("Error: ", httpException);
            String errorDetail = httpException.getStatusCode() + " " + httpException.getStatusText();
            String responseBody = httpException.getResponseBodyAsString();
            if (StringUtils.isNotBlank(responseBody)) {
                errorDetail += " - " + responseBody;
            }
            model.addAttribute("errorMsg", errorDetail);
            return "error";
        } catch (Exception unhandledException) {
            logger.error("Error: ", unhandledException);
            if(showUnhandledErrors) model.addAttribute("errorMsg", unhandledException);
            else model.addAttribute("errorMsg", "Internal Server Error");
            return "error";
        }
    }



    private String getClientCredentialsToken() throws ParseException, JOSEException {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();

        body.add("grant_type", "client_credentials");
        body.add("client_id", clientId);
        body.add("client_secret", clientSecret);
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);
        String tokenEndpoint = openIDConfiguration.getTokenEndpoint();
        TokenResponse tokenResponse = restTemplate.postForObject(tokenEndpoint, request, TokenResponse.class);
        String accessToken = tokenResponse.getAccess_token();
        return accessToken;
    }

    private static final Pattern TIME_PATTERN = Pattern.compile("(\\d+-\\d{2}-\\d{2}).*(\\d{2}:\\d{2}:\\d{2}).*");

    /**
     * convert date time format: 2019-09-18T10:55:20 -> 2019-09-18T10:55:20Z
     * @param time date time from form submit
     * @return convert format date time
     */
    private String convertTimeFormat(String time) {
        return TIME_PATTERN.matcher(time).replaceFirst("$1T$2Z");
    }

    public static String localToGMT(String dateString) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        sdf.setTimeZone(TimeZone.getDefault());
        Date date = null;
        try {
            date = sdf.parse(dateString);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        SimpleDateFormat sdfGmt = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        sdfGmt.setTimeZone(TimeZone.getTimeZone("UTC"));
        String gmtString = sdfGmt.format(date);
        return gmtString;
    }

    private String encodeQueryParam(String value) {
        try {
            return URLEncoder.encode(value, "UTF-8");
        } catch (UnsupportedEncodingException impossible) {
            logger.error("charset name is wrong", impossible);
            return null;
        }
    }

    /**
     * calculate the c_hash, at_hash, s_hash, the logic as follow:
     * 1. Using the hash algorithm specified in the alg claim in the ID Token header
     * 2. hash the octets of the ASCII representation of the code
     * 3. Base64url-encode the left-most half of the hash.
     *
     * @param str to calculate hash value
     * @param idTokenAlg the sign algorithm name of sign id token
     * @return hash value
     */
    private String buildHash(String str, String idTokenAlg) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance(idTokenAlg);
        byte[] asciiValue = str.getBytes(StandardCharsets.US_ASCII);
        byte[] encodedHash = md.digest(asciiValue);
        byte[] halfOfEncodedHash = Arrays.copyOf(encodedHash, (encodedHash.length / 2));
        return Base64.getUrlEncoder().withoutPadding().encodeToString(halfOfEncodedHash);
    }
}
