package com.openbankproject.hydra.auth.controller;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
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
import sh.ory.hydra.model.WellKnown;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.servlet.http.HttpSession;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.ParseException;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Controller
public class IndexController {
    private static final Logger logger = LoggerFactory.getLogger(IndexController.class);

    // if need idToken, add openid; if need refreshToken, add offline
    @Value("openid,offline,${oauth2.client_scope}")
    private LinkedHashSet<String> allScopes;

    @Value("${oauth2.redirect_uri}")
    private String redirectUri;
    @Value("${oauth2.client_id}")
    private String clientId;
    @Value("${oauth2.client_secret}")
    private String clientSecret;

    @Value("${obp.base_url}")
    private String obpBaseUrl;
    @Value("${obp.base_url}/obp/v4.0.0/users/current")
    private String currentUserUrl;
    @Value("${obp.base_url}/obp/v4.0.0/banks")
    private String getBanksUrl;
    @Value("${endpoint.path.prefix}/account-access-consents")
    private String createConsentsUrl;

    @Value("${oauth2.jwk_private_key:}")
    private String jwkPrivateKey;

    @Resource
    private RestTemplate restTemplate;
    @Resource
    private WellKnown openIDConfiguration;

    private ECDSASigner eCDSASigner;

    @PostConstruct
    private void initiate() throws ParseException, JOSEException {
        if(StringUtils.isNotBlank(jwkPrivateKey)) {
            final ECKey ecKey = (ECKey) JWK.parse(jwkPrivateKey);
            eCDSASigner = new ECDSASigner(ecKey);
        }
    }

    @GetMapping({"/", "/index", "index.html"})
    public String index(Model model) throws ParseException, JOSEException {
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


    @PostMapping(value="/request_consents", params = {"bank", "consents", "transaction_from_time", "transaction_to_time", "expiration_time"})
    public String requestConsents(@RequestParam("bank") String bankId,
                                  @RequestParam String[] consents,
                                  @RequestParam String transaction_from_time,
                                  @RequestParam String transaction_to_time,
                                  @RequestParam String expiration_time,
                                  HttpSession session
                                  ) throws UnsupportedEncodingException, ParseException, JOSEException {
        final String consentId;
        {   // Create Account Access Consents
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
        Map<String, String> queryParam = new LinkedHashMap<>();
        queryParam.put("client_id", clientId);
        queryParam.put("response_type", "code");
        // include OBP scopes, add OAuth2 and OIDC related scope: "openid" and "offline"
        consents = ArrayUtils.addAll(new String[]{"openid", "offline"}, consents);
        String scope = Stream.of(consents)
                .distinct()
                .map(this::encodeQueryParam)
                .collect(Collectors.joining("+"));

        queryParam.put("scope", scope);
        String encodeRedirectUri = URLEncoder.encode(redirectUri, "UTF-8");
        queryParam.put("redirect_uri", encodeRedirectUri);
        String state = UUID.randomUUID().toString();
        queryParam.put("state", state);

        // the parameter consent_id and bank_id are mandatory, these two parameter is not standard parameter of OAuth2 and OIDC
        queryParam.put("consent_id", consentId);
        queryParam.put("bank_id", bankId);
        // TODO the acr_values is just temp example value, can be space split values, need check and supply real values.
        queryParam.put("acr_values", "urn:openbankproject:psd2:sca");

        String queryParamStr = queryParam.entrySet().stream().map(it -> it.getKey() + "=" + it.getValue()).collect(Collectors.joining("&"));
        String authorizationEndpoint = openIDConfiguration.getAuthorizationEndpoint();
        String redirectUrl = "redirect:" + authorizationEndpoint + "?" + queryParamStr;
        SessionData.setBankId(session, bankId);
        return redirectUrl;
    }

    @GetMapping(value={"/main", "main.html"}, params="code")
    public String main(@RequestParam("code") String code, @RequestParam("scope") String scope,
                       Model model,
                       HttpSession session) throws ApiException, ParseException, JOSEException {
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
            if(StringUtils.isBlank(jwkPrivateKey)) {
                body.add("client_secret", clientSecret);
            } else {
                body.add("client_assertion_type", "urn:ietf:params:oauth:client-assertion-type:jwt-bearer");
                body.add("client_assertion", this.buildJwt());
            }

            HttpEntity<MultiValueMap> request = new HttpEntity<>(body, headers);
            String tokenEndpoint = openIDConfiguration.getTokenEndpoint();
            TokenResponse tokenResponse = restTemplate.postForObject(tokenEndpoint, request , TokenResponse.class);

            SessionData.setIdToken(session, tokenResponse.getId_token());
            SessionData.setAccessToken(session, tokenResponse.getAccess_token());
            SessionData.setRefreshToken(session, tokenResponse.getRefresh_token());

            logger.debug("idToken:" + tokenResponse.getId_token());
            logger.debug("accessToken:" + tokenResponse.getAccess_token());
        }

        { // fetch user information
            HttpHeaders headers = new HttpHeaders();
            String accessToken = SessionData.getAccessToken(session);
            headers.setBearerAuth(accessToken);
            HttpEntity<String> entity = new HttpEntity<>(headers);
            ResponseEntity<UserInfo> userInfoResponse = restTemplate.exchange(currentUserUrl, HttpMethod.GET, entity, UserInfo.class);
            SessionData.setUserInfo(session, userInfoResponse.getBody());
            logger.debug("login success user:" + userInfoResponse.getBody().getUsername());
        }

        return "redirect:/main";
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



    private String getClientCredentialsToken() throws ParseException, JOSEException {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();

        body.add("grant_type", "client_credentials");
        body.add("client_id", clientId);
        if(StringUtils.isBlank(jwkPrivateKey)) {
            body.add("client_secret", clientSecret);
        } else {
            body.add("client_assertion_type", "urn:ietf:params:oauth:client-assertion-type:jwt-bearer");
            body.add("client_assertion", this.buildJwt());
        }
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

    private String encodeQueryParam(String value) {
        try {
            return URLEncoder.encode(value, "UTF-8");
        } catch (UnsupportedEncodingException impossible) {
            logger.error("charset name is wrong", impossible);
            return null;
        }
    }

    /**
     * create client_assertion
     * @return
     * @throws ParseException
     */
    private String buildJwt() throws ParseException, JOSEException {
        final JWK jwk = JWK.parse(jwkPrivateKey);
        // JWT claims
        //iss: REQUIRED. Issuer. This MUST contain the client_id of the OAuth Client.
        //sub: REQUIRED. Subject. This MUST contain the client_id of the OAuth Client.
        //aud: REQUIRED. Audience. The aud (audience) Claim. Value that identifies the Authorization Server (ORY Hydra) as an intended audience. The Authorization Server MUST verify that it is an intended audience for the token. The Audience SHOULD be the URL of the Authorization Server's Token Endpoint.
        //jti: REQUIRED. JWT ID. A unique identifier for the token, which can be used to prevent reuse of the token. These tokens MUST only be used once, unless conditions for reuse were negotiated between the parties; any such negotiation is beyond the scope of this specification.
        //exp: REQUIRED. Expiration time on or after which the ID Token MUST NOT be accepted for processing.
        //iat: OPTIONAL. Time at which the JWT was issued.
        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .issuer(clientId)
                .subject(clientId)
                .audience(openIDConfiguration.getTokenEndpoint())
                .jwtID(UUID.randomUUID().toString())
                .expirationTime(new Date(new Date().getTime() + 60 * 1000))
                .issueTime(new Date())
                .build();

        // Create JWT for ES256K alg
        SignedJWT jwt = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.ES256)
                        .keyID(jwk.getKeyID())
                        .build(),
                claimsSet);

        // Sign with private EC key
        jwt.sign(eCDSASigner);

        // To serialize to compact form, produces something like
        // eyJhbGciOiJSUzI1NiJ9.SW4gUlNBIHdlIHRydXN0IQ.IRMQENi4nJyp4er2L
        // mZq3ivwoAjqa1uUkSBKFIX7ATndFF5ivnt-m8uApHO4kfIFOrW7w2Ezmlg3Qd
        // maXlS9DhN0nUk_hGI3amEjkKd0BWYCB8vfUbUv0XGjQip78AI4z1PrFRNidm7
        // -jPDm5Iq0SZnjKjCNS5Q15fokXZc8u0A
        return jwt.serialize();
    }
}
