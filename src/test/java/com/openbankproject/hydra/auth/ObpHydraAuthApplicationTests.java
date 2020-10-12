package com.openbankproject.hydra.auth;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;

@SpringBootTest
@ActiveProfiles("local")
@Disabled
class ObpHydraAuthApplicationTests {
    @Resource
    private RestTemplate restTemplate;

    @Test
    void contextLoads() {
        ResponseEntity<String> forEntity = restTemplate.getForEntity("https://api-mtls.ofpilot.com/obp/v4.0.0/banks", String.class);
        ResponseEntity<String> forEntity2 = restTemplate.getForEntity("https://oauth2.api-mtls.ofpilot.com/hydra-public/.well-known/openid-configuration", String.class);
//        ResponseEntity<String> forEntity = restTemplate.getForEntity("https://api-mtls.ofpilot.com/mx-open-finance/v0.0.1/accounts", String.class);
        System.out.println("------------------");
    }

}
