package com.openbankproject.hydra.auth;

import com.openbankproject.hydra.auth.VO.WellKnown;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClientException;
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
        try {
            openIDConfiguration = restTemplate.getForObject(wellKnownUrl, WellKnown.class);
        } catch (RestClientException e) {
            // This runs during context refresh, so the raw exception surfaces as a
            // BeanCreationException with a stack trace that buries the one useful fact:
            // the provider is not answering at this URL. Say that plainly, then rethrow
            // so startup still fails fast -- the app cannot work without OIDC.
            log.error("");
            log.error("Cannot reach the OIDC provider at {}", wellKnownUrl);
            log.error("  {}", e.getMessage());
            log.error("  Start your OIDC provider, or point this app at a different one:");
            log.error("    --oauth2.public_url=http://localhost:7070/realms/master");
            log.error("    (or set the KEYCLOAK_PUBLIC_URL environment variable)");
            log.error("");
            throw e;
        }
        log.info("Loaded OIDC well-known configuration from {}", wellKnownUrl);
    }

    @Bean
    public WellKnown openIDConfiguration() {
        return this.openIDConfiguration;
    }
}
