package com.openbankproject.hydra.auth;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.crypto.bc.BouncyCastleProviderSingleton;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.openbankproject.hydra.auth.VO.WellKnown;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.security.Security;
import java.text.ParseException;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

@Configuration
public class HydraConfig {
    Logger log = LoggerFactory.getLogger(HydraConfig.class);
    @Value("${oauth2.public_url}/.well-known/openid-configuration")
    private String hydraWellKnownUrl;

    @Value("${oauth2.client_id}")
    private String clientId;

    // default is empty string
    @Value("${oauth2.client_secret:}")
    private String clientSecret;
    @Value("${oauth2.jwk_private_key:}")
    private String jwkPrivateKey;
    @Value("${oauth2.jws_alg:ES256}")
    private String jwsAlg;

    @Resource
    private RestTemplate restTemplate;

    private JWSSigner jwsSigner;

    private JWK jwk;

    private WellKnown openIDConfiguration;

    @PostConstruct
    private void initiate() throws ParseException, JOSEException {
        if(StringUtils.isNotBlank(jwkPrivateKey)) {
            jwk = JWK.parse(jwkPrivateKey);
            if(jwsAlg.startsWith("ES")) {
                jwsSigner = new ECDSASigner((ECKey)jwk);
            } else {
                final RSAKey privateKey = (RSAKey)jwk;
                jwsSigner = new RSASSASigner(privateKey);
            }
            if(jwsAlg.startsWith("PS")) {
                Security.addProvider(BouncyCastleProviderSingleton.getInstance());
            }
        }

        final long count = Stream.of(jwkPrivateKey, clientSecret).filter(StringUtils::isNotBlank).count();
        if(count != 1L) {
            throw new IllegalStateException("Properties value oauth2.jwk_private_key and oauth2.client_secret must only one have value.");
        }
        openIDConfiguration = restTemplate.getForObject(hydraWellKnownUrl, WellKnown.class);
    }


    @Bean
    public WellKnown openIDConfiguration() {
        return this.openIDConfiguration;
    }

    /**
     * Whether the hydra client is public:
     * if token_endpoint_auth_methods_supported=private_key_jwt, return ture
     * @return
     */
    public boolean isPublicClient() {
        return StringUtils.isNotBlank(this.jwkPrivateKey);
    }

    /**
     * create client_assertion
     * @return
     * @throws ParseException when oauth2.jwk_private_key value is not valid jwk private key
     */
    public String buildClientAssertion() {
        // JWT claims
        //iss: REQUIRED. Issuer. This MUST contain the client_id of the OAuth Client.
        //sub: REQUIRED. Subject. This MUST contain the client_id of the OAuth Client.
        //aud: REQUIRED. Audience. The aud (audience) Claim. Value that identifies the Authorization Server (ORY Hydra) as an intended audience. The Authorization Server MUST verify that it is an intended audience for the token. The Audience SHOULD be the URL of the Authorization Server's Token Endpoint.
        //jti: REQUIRED. JWT ID. A unique identifier for the token, which can be used to prevent reuse of the token. These tokens MUST only be used once, unless conditions for reuse were negotiated between the parties; any such negotiation is beyond the scope of this specification.
        //exp: REQUIRED. Expiration time on or after which the ID Token MUST NOT be accepted for processing.
        //iat: OPTIONAL. Time at which the JWT was issued.
        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .issuer(clientId)
                .subject(clientId)
                .audience(openIDConfiguration.getTokenEndpoint())
                .jwtID(UUID.randomUUID().toString())
                .expirationTime(new Date(new Date().getTime() + 60 * 1000))
                .issueTime(new Date())
                .build();

        return signClaims(claimsSet);
    }

    public String buildRequestObject(Map<String, String> queryParam) {
        /* example
        {
            "redirect_uri":"http://localhost:8081/main.html",
            "response_type":"code id_token",
            "client_id":"g4zvglgxz4srknsywzrf1alszrxc3em2ompkz2ap",
            "scope":"openid offline ReadAccountsBasic",
            "nonce":"n-0S6_WzA2Mj"
        }
        */
        final String redirect_uri = queryParam.get("redirect_uri");
        String redirectUri = null;
        try {
            redirectUri = URLDecoder.decode(redirect_uri, "UTF-8");
        } catch (UnsupportedEncodingException impossible) {
            log.error("Wrong encoding name", impossible);
        }
        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .claim("redirect_uri", redirectUri)
                .claim("response_type", queryParam.get("response_type").replace("+", " "))
                .claim("client_id", queryParam.get("client_id"))
                .claim("scope", queryParam.get("scope").replace("+", " "))
                .claim("state", queryParam.get("state"))
                .claim("exp", queryParam.get(Instant.now().getEpochSecond()) + 60) // expire after 1 minute.
                .claim("nonce", queryParam.get("nonce"))
                .build();

        return signClaims(claimsSet);
    }

    private String signClaims(JWTClaimsSet claimsSet) {
        SignedJWT jwt = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.parse(jwsAlg))
                        .keyID(jwk.getKeyID())
                        .build(),
                claimsSet);

        // Sign with private EC key
        try {
            jwt.sign(jwsSigner);
        } catch (JOSEException e) {
            ExceptionUtils.rethrow(e);
        }
        return jwt.serialize();
    }
}
