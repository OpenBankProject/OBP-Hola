package com.openbankproject.hydra.auth;

import com.openbankproject.hydra.auth.VO.WellKnown;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

@Configuration
public class ObpOidcConfig {
    Logger log = LoggerFactory.getLogger(ObpOidcConfig.class);

    @Value("${oauth2.public_url:http://localhost:9000/obp-oidc}/.well-known/openid-configuration")
    private String wellKnownUrl;

    @Resource
    private RestTemplate restTemplate;

    private WellKnown openIDConfiguration;

    @PostConstruct
    private void initiate() {
        openIDConfiguration = restTemplate.getForObject(wellKnownUrl, WellKnown.class);
        log.info("Loaded OBP-OIDC well-known configuration from {}", wellKnownUrl);
    }

    @Bean
    public WellKnown openIDConfiguration() {
        return this.openIDConfiguration;
    }
}
