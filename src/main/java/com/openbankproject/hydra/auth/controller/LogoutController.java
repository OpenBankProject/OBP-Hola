package com.openbankproject.hydra.auth.controller;

import com.openbankproject.hydra.auth.VO.SessionData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import sh.ory.hydra.ApiException;
import sh.ory.hydra.api.PublicApi;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

@Controller
public class LogoutController {
    private static Logger logger = LoggerFactory.getLogger(LogoutController.class);

    @Value("${oauth2.redirect_uri}")
    private String redirectUri;

    @Value("${oauth2.public_url}/oauth2/sessions/logout")
    private String hydraLogoutUrl;

    @Resource
    private PublicApi hydraPublic;

    @GetMapping("/logout")
    public String logout(HttpSession session) throws ApiException, UnsupportedEncodingException {
        if(SessionData.isAuthenticated(session)){
            String accessToken = SessionData.getAccessToken(session);
            String idToken = SessionData.getIdToken(session);
            String encodeRedirectUri = URLEncoder.encode(redirectUri, "UTF-8");
            hydraPublic.revokeOAuth2Token(accessToken) ;
            session.invalidate();
            return "redirect:"+ hydraLogoutUrl + "?post_logout_redirect_uri=" + encodeRedirectUri + "&id_token_hint="+idToken;
        }
        return "redirect:" + redirectUri;
    }
}