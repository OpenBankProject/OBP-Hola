package com.openbankproject.hydra.auth.controller;

import com.openbankproject.hydra.auth.VO.*;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestTemplate;
import sh.ory.hydra.ApiException;
import sh.ory.hydra.api.AdminApi;
import sh.ory.hydra.api.PublicApi;
import sh.ory.hydra.model.OAuth2TokenIntrospection;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Controller
public class IndexController {
    private static final Logger logger = LoggerFactory.getLogger(IndexController.class);

    // if need idToken, add openid; if need refreshToken, add offline
    @Value("openid,offline,${oauth2.client_scope}")
    private LinkedHashSet<String> allScopes;

    @Value("${oauth2.public_url}/oauth2/token")
    private String hydraTokenUrl;
    @Value("${oauth2.redirect_uri}")
    private String redirectUri;
    @Value("${oauth2.client_id}")
    private String clientId;
    @Value("${oauth2.client_secret}")
    private String clientSecret;
    @Value("${oauth2.authenticate_url}")
    private String authenticateUrl;


    @Value("${obp.base_url}")
    private String obpBaseUrl;
    @Value("${obp.base_url}/obp/v4.0.0/users/current")
    private String currentUserUrl;
    @Value("${obp.base_url}/obp/v4.0.0/banks")
    private String getBanksUrl;
    @Value("${obp.base_url}/mx-open-finance/v0.0.1/account-access-consents")
    private String createConsentsUrl;
    @Value("${obp.base_url}/obp/v4.0.0/banks/BANK_ID/accounts-held")
    private String getAccountsUrl;
    @Value("${obp.base_url}/obp/v4.0.0/banks/BANK_ID/accounts/ACCOUNT_ID/account-access")
    private String resetAccessViewUrl;

    @Resource
    private RestTemplate restTemplate;

    @Resource
    private AdminApi hydraAdmin;

    @Resource
    private PublicApi hydraPublic;

    @GetMapping({"/", "/index", "index.html"})
    public String index(Model model) throws ApiException {
        model.addAttribute("obp_url", obpBaseUrl);
        {// initiate consent names
            // exclude "openid" and "offline", they are used by hydra
            String[] consents = allScopes.stream()
                    .filter(it -> !"openid".equals(it) && !"offline".equals(it))
                    .toArray(String[]::new);
            model.addAttribute("consents", consents);
        }
        { // initiate all bank names and bank ids
            Banks banks = restTemplate.getForObject(getBanksUrl, Banks.class);
            model.addAttribute("banks", banks.getBanks());
        }
        return "index";
    }


    @PostMapping(value="/request_consents", params = {"bank", "consents", "transaction_from_time", "transaction_to_time"})
    public String requestConsents(@RequestParam("bank") String bankId,
                                  @RequestParam String[] consents,
                                  @RequestParam String transaction_from_time,
                                  @RequestParam String transaction_to_time,
                                  @RequestParam String expiration_time
                                  ) throws UnsupportedEncodingException {
        final String consentId;
        {   // create consents
            String clientCredentialsToken = getClientCredentialsToken();
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(clientCredentialsToken);

            // TODO it should have relation with rememberMe time
            String expirationDateTime = convertTimeFormat(expiration_time);
            ConsentPostBodyMXOFV001 body = new ConsentPostBodyMXOFV001(
                    bankId,
                    consents,
                    convertTimeFormat(transaction_from_time),
                    convertTimeFormat(transaction_to_time),
                    expirationDateTime);

            HttpEntity<ConsentPostBodyMXOFV001> request = new HttpEntity<>(body, headers);

            Map response = restTemplate.postForObject(createConsentsUrl, request, Map.class);
            consentId = ((Map<String, String>) response.get("Data")).get("ConsentId");
        }
        //{"client_id", "bank_id", "consent_id", "response_type=code", "scope", "redirect_uri", "state"})
        Map<String, String> queryParam = new HashMap<>();
        queryParam.put("client_id", clientId);
        queryParam.put("bank_id", bankId);
        queryParam.put("consent_id", consentId);
        queryParam.put("response_type", "code");
        // keep the selected consents, only if it exists in allScopes, keep "openid" and "offline" if it exists in allScopes
        String scope =
                allScopes.stream().filter(it ->
                    "openid".equals(it) || "offline".equals(it) || ArrayUtils.contains(consents, it)
                ).collect(Collectors.joining("+"));

        queryParam.put("scope", scope);
        String encodeRedirectUri = URLEncoder.encode(redirectUri, "UTF-8");
        queryParam.put("redirect_uri", encodeRedirectUri);
        String state = UUID.randomUUID().toString();
        queryParam.put("state", state);

        String queryParamStr = queryParam.entrySet().stream().map(it -> it.getKey() + "=" + it.getValue()).collect(Collectors.joining("&"));
        String redirectUrl = "redirect:" + authenticateUrl + "?" + queryParamStr;

        return redirectUrl;
    }

    @GetMapping(value={"/main", "main.html"}, params="code")
    public String callBackMain(@RequestParam("code") String code, @RequestParam("scope") String scope, @RequestParam("state") String state,
                       Model model,
                       HttpSession session) throws ApiException {
        model.addAttribute("obp_url", obpBaseUrl);
        // when repeat call with same code, just do nothing.
        if(code.equals(SessionData.getCode(session))) {
            return "accounts";
        }
        String[] consentArray = StringUtils.split(scope, ' ');
        String[] selectedConsents = ArrayUtils.removeElements(consentArray, "openid", "offline");
        SessionData.setSelectConsents(session,selectedConsents);
        SessionData.setCode(session, code);
        // get tokens use code
        {
            HttpHeaders headers = new HttpHeaders();
            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("grant_type", "authorization_code");
            body.add("code", code);
            body.add("redirect_uri", redirectUri);
            body.add("client_id", clientId);
            body.add("client_secret", clientSecret);

            HttpEntity<MultiValueMap> request = new HttpEntity<>(body, headers);

            TokenResponse tokenResponse = restTemplate.postForObject(hydraTokenUrl, request , TokenResponse.class);

            SessionData.setIdToken(session, tokenResponse.getId_token());
            SessionData.setAccessToken(session, tokenResponse.getAccess_token());
            SessionData.setRefreshToken(session, tokenResponse.getRefresh_token());

            logger.debug("idToken:" + tokenResponse.getId_token());
            logger.debug("accessToken:" + tokenResponse.getAccess_token());
        }

        HttpHeaders headers = new HttpHeaders();
        String accessToken = SessionData.getAccessToken(session);
        headers.setBearerAuth(accessToken);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        { // fetch user information
            ResponseEntity<UserInfo> userInfoResponse = restTemplate.exchange(currentUserUrl, HttpMethod.GET, entity, UserInfo.class);
            SessionData.setUserInfo(session, userInfoResponse.getBody());
        }
        {
            OAuth2TokenIntrospection oAuth2TokenIntrospection = hydraAdmin.introspectOAuth2Token(SessionData.getAccessToken(session), null);
            Map<String, String> accessExt = (Map<String, String>) oAuth2TokenIntrospection.getExt();
            String bankId = accessExt.get("bank_id");
//            String consentId = accessExt.get("consent_id");
            SessionData.setBankId(session, bankId);
        }
        {
            String bankId = SessionData.getBankId(session);
            ResponseEntity<Accounts> accounts = restTemplate.exchange(getAccountsUrl.replace("BANK_ID", bankId), HttpMethod.GET, entity, Accounts.class);
            model.addAttribute("accounts", accounts.getBody().getAccounts());

            SessionData.setAllAccountIds(session, accounts.getBody().accountIds());
        }

        return "accounts";
    }

    @PostMapping("/reset_access_to_views")
    public String resetAccessToViews(@RequestParam("accounts") String[] accountIs, HttpSession session) {
        String bankId = SessionData.getBankId(session);
        String[] selectConsents = SessionData.getSelectConsents(session);

        HttpHeaders headers = new HttpHeaders();
        String accessToken = SessionData.getAccessToken(session);
        headers.setBearerAuth(accessToken);

        { // process selected accounts
            AccessToViewRequest body = new AccessToViewRequest(selectConsents);
            HttpEntity<AccessToViewRequest> entity = new HttpEntity<>(body, headers);
            for (String accountId : accountIs) {
                String url = resetAccessViewUrl.replace("BANK_ID", bankId).replace("ACCOUNT_ID", accountId);
                restTemplate.exchange(url, HttpMethod.PUT, entity, HashMap.class);
            }
        }

        { // process not selected accounts
            String[] notSelectAccountIds = ArrayUtils.removeElements(SessionData.getAllAccountIds(session), accountIs);
            AccessToViewRequest body = new AccessToViewRequest(ArrayUtils.EMPTY_STRING_ARRAY);
            HttpEntity<AccessToViewRequest> entity = new HttpEntity<>(body, headers);
            for (String accountId : notSelectAccountIds) {
                String url = resetAccessViewUrl.replace("BANK_ID", bankId).replace("ACCOUNT_ID", accountId);
                restTemplate.exchange(url, HttpMethod.PUT, entity, HashMap.class);
            }
        }

        return "redirect:/main.html";
    }

    @GetMapping(value={"/main", "main.html"}, params="!code")
    public String main(HttpSession session, Model model) {
        model.addAttribute("obp_url", obpBaseUrl);
        UserInfo user = SessionData.getUserInfo(session);
        if(user == null) {
            return "redirect:/index.html";
        }
        model.addAttribute("user", user);

        return "main";
    }



    private String getClientCredentialsToken() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();

        body.add("grant_type", "client_credentials");
        body.add("client_id", clientId);
        body.add("client_secret", clientSecret);
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

        TokenResponse tokenResponse = restTemplate.postForObject(hydraTokenUrl, request, TokenResponse.class);
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
}
