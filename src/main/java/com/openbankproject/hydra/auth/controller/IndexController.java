package com.openbankproject.hydra.auth.controller;

import com.openbankproject.hydra.auth.VO.Account;
import com.openbankproject.hydra.auth.VO.AccountDataValue;
import com.openbankproject.hydra.auth.VO.TokenResponse;
import com.openbankproject.hydra.auth.VO.UserInfo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpSession;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Controller
public class IndexController {

    @Value("openid,offline,${oauth2.client_scope}")
    private List<String> scopeList;

    @Value("${oauth2.public_url}/oauth2/auth?client_id=${oauth2.client_id}&response_type=code")
    private String hydraLoginUrl;
    @Value("${oauth2.public_url}/oauth2/token")
    private String hydraTokenUrl;
    @Value("${oauth2.redirect_uri}")
    private String redirectUri;
    @Value("${oauth2.client_id}")
    private String clientId;
    @Value("${oauth2.client_secret}")
    private String clientSecret;

    @Value("${obp.base_url}/obp/v4.0.0/users/current")
    private String currentUserUrl;
    @Value("${obp.base_url}/mx-open-finance/v0.0.1/accounts")
    private String getAccountsUrl;

    private String scope;

    @PostConstruct
    private void initiate() {
        scope = scopeList.stream().distinct().collect(Collectors.joining("+"));
    }

    @GetMapping({"/", "/index", "index.html"})
    public String index(Model model) throws UnsupportedEncodingException {
        String state = UUID.randomUUID().toString();

        String redirect_uri = URLEncoder.encode(redirectUri, "UTF-8");

        String loginUrl = hydraLoginUrl +
                "&state=" + state +
                "&scope=" + scope +
                "&redirect_uri=" + redirect_uri;
        model.addAttribute("loginUrl", loginUrl);

       return "index";
    }
    @GetMapping(value={"/main", "main.html"}, params="code")
    public String callBackMain(@RequestParam("code") String code, @RequestParam("scope") String scope, @RequestParam("state") String state,
                       HttpSession session) {
        RestTemplate restTemplate = new RestTemplate();

        // get tokens use code
        {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> body= new LinkedMultiValueMap<>();
            body.add("grant_type", "authorization_code");
            body.add("code", code);
            body.add("redirect_uri", redirectUri);
            body.add("client_id", clientId);
            body.add("client_secret", clientSecret);

            HttpEntity<MultiValueMap> request = new HttpEntity<>(body, headers);

            TokenResponse tokenResponse = restTemplate.postForObject(hydraTokenUrl, request , TokenResponse.class);
            session.setAttribute("tokenResponse", tokenResponse);

            session.setAttribute("idToken", tokenResponse.getId_token());
            session.setAttribute("access_token", tokenResponse.getAccess_token());
            session.setAttribute("expires_in", tokenResponse.getExpires_in());
            session.setAttribute("refresh_token", tokenResponse.getRefresh_token());
            session.setAttribute("token_type", tokenResponse.getToken_type());

            session.setAttribute("scope", scope);
            session.setAttribute("state", state);

            System.out.println("idToken:" + tokenResponse.getId_token());
            System.out.println("accessToken:" + tokenResponse.getAccess_token());
        }

        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Authorization: Bearer " + session.getAttribute("access_token"));
        headers.add("Content-Type", "application/json");
        HttpEntity<String> entity = new HttpEntity<>(headers);
        ResponseEntity<UserInfo> userInfoResponse = restTemplate.exchange(currentUserUrl, HttpMethod.GET, entity, UserInfo.class);
        session.setAttribute("user", userInfoResponse.getBody());

        return "redirect:/main.html";
    }

    @GetMapping(value={"/main", "main.html"}, params="!code")
    public String main(HttpSession session, Model model) {
        UserInfo user = (UserInfo) session.getAttribute("user");
        if(user == null) {
            return "redirect:/index.html";
        }
        model.addAttribute("user", user);

        RestTemplate restTemplate = new RestTemplate();
        String accessToken = (String) session.getAttribute("access_token");
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Authorization: Bearer " + accessToken);
        headers.add("Content-Type", "application/json");
        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<AccountDataValue> exchange = restTemplate.exchange(getAccountsUrl, HttpMethod.GET, entity, AccountDataValue.class);
        AccountDataValue accountDataValue = exchange.getBody();
        System.out.println("account:" + accountDataValue);
        List<Account> accounts = new ArrayList<>() ;
        if(accountDataValue.getData() != null) {
            accounts = accountDataValue.getData().getAccount();
            System.out.println("accounts:" + accounts);
        }
        model.addAttribute("accounts", accounts);
        return "main";
    }
}
