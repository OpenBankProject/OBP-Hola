package com.openbankproject.hydra.auth.controller;

import com.openbankproject.hydra.auth.HydraConfig;
import com.openbankproject.hydra.auth.VO.SessionData;
import com.openbankproject.hydra.auth.VO.WellKnown;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.Map;

@Controller
public class LogoutController {

    @Value("${oauth2.redirect_uri}")
    private String redirectUri;

    @Value("${oauth2.client_id}")
    private String clientId;
    // default is empty string
    @Value("${oauth2.client_secret:}")
    private String clientSecret;

    @Resource
    private WellKnown openIDConfiguration;
    @Resource
    private RestTemplate restTemplate;
    @Resource
    private HydraConfig hydraConfig;

    @GetMapping("/logout")
    public String logout(HttpSession session) throws UnsupportedEncodingException {
        String encodeRedirectUri = URLEncoder.encode(redirectUri, "UTF-8");
        final String idToken = SessionData.getIdToken(session);
        final String state = SessionData.getState(session);
        final String accessToken = SessionData.getAccessToken(session);
        session.invalidate();
        if (StringUtils.isNotBlank(accessToken)) {
            final String revocationEndpoint = openIDConfiguration.getRevocationEndpoint();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("token", accessToken);
            if (hydraConfig.isPublicClient()) {
                body.add("client_assertion_type", "urn:ietf:params:oauth:client-assertion-type:jwt-bearer");
                body.add("client_assertion", this.hydraConfig.buildClientAssertion());
            } else {
                body.add("client_id", clientId);
                body.add("client_secret", clientSecret);
            }
            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);
            restTemplate.postForObject(revocationEndpoint, request, Map.class);
        }
        if (StringUtils.isNoneBlank(idToken, state)) {
            return "redirect:" + openIDConfiguration.getEndSessionEndpoint() + "?state=" + state + "&post_logout_redirect_uri=" + encodeRedirectUri + "&id_token_hint=" + idToken;
        }
        return "redirect:" + redirectUri + "?post_logout_redirect_uri=" + encodeRedirectUri;
    }
}