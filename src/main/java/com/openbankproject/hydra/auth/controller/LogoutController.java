package com.openbankproject.hydra.auth.controller;

import com.openbankproject.hydra.auth.VO.SessionData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

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

    @GetMapping("/logout")
    public String logout(HttpSession session) throws UnsupportedEncodingException {
        if(SessionData.isAuthenticated(session)){
            String idToken = SessionData.getIdToken(session);
            session.invalidate();
            String encodeRedirectUri = URLEncoder.encode(redirectUri, "UTF-8");
            return "redirect:"+ hydraLogoutUrl + "?post_logout_redirect_uri=" + encodeRedirectUri + "&id_token_hint="+idToken;
        } else {
            return "redirect:" + redirectUri;
        }
    }
}