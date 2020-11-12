package com.openbankproject.hydra.auth;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

@Configuration
public class WebSecurityConfiguration extends WebSecurityConfigurerAdapter {
    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.csrf().csrfTokenRepository(new CookieCsrfTokenRepository().withHttpOnlyFalse())
                .requireCsrfProtectionMatcher(
                        httpServletRequest -> !httpServletRequest.getMethod().equalsIgnoreCase("GET")
                )
                .and().authorizeRequests().anyRequest().permitAll();
    }
}
