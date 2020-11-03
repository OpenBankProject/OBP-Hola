package com.openbankproject.hydra.auth.controller;

import com.openbankproject.hydra.auth.VO.SessionData;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import sh.ory.hydra.model.WellKnown;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

@Controller
public class LogoutController {
    @Value("${oauth2.redirect_uri}")
    private String redirectUri;
    @Resource
    private WellKnown openIDConfiguration;

    @GetMapping("/logout")
    public String logout(HttpSession session) throws UnsupportedEncodingException {
        String encodeRedirectUri = URLEncoder.encode(redirectUri, "UTF-8");
        final String idToken = SessionData.getIdToken(session);
        final String state = SessionData.getState(session);
        session.invalidate();
        if(StringUtils.isNoneBlank(idToken, state)){
            return "redirect:"+ openIDConfiguration.getEndSessionEndpoint() + "?state=" + state + "&post_logout_redirect_uri="+encodeRedirectUri + "&id_token_hint=" + idToken;
        }
        return "redirect:" + redirectUri + "?post_logout_redirect_uri=" + encodeRedirectUri;
    }
}