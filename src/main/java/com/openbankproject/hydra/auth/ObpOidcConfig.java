package com.openbankproject.hydra.auth;

import com.openbankproject.hydra.auth.VO.WellKnown;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.Map;

@Configuration
@ConditionalOnProperty(name = "oauth2.provider", havingValue = "obp-oidc", matchIfMissing = true)
public class ObpOidcConfig implements OIDCProvider {
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

    /**
     * OBP-OIDC uses client_secret authentication, not private_key_jwt.
     */
    @Override
    public boolean isPublicClient() {
        return false;
    }

    @Override
    public String buildClientAssertion() {
        throw new UnsupportedOperationException("OBP-OIDC does not support private_key_jwt client assertions");
    }

    @Override
    public String buildRequestObject(Map<String, String> queryParam) {
        throw new UnsupportedOperationException("OBP-OIDC does not support signed request objects");
    }
}
