package com.openbankproject;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.crypto.bc.BouncyCastleProviderSingleton;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.security.cert.CertificateException;
import java.text.ParseException;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

@Configuration
public class ClientConfig {
    Logger log = LoggerFactory.getLogger(ClientConfig.class);

    private JWK jwkClient;

    public JWK getJwkClient() {
        return jwkClient;
    }

    @PostConstruct
    private void initiate() throws ParseException, JOSEException {
        // Load the key store from file
        try {
            // Specify the key store type, e.g. JKS
            KeyStore keyStore = KeyStore.getInstance("JKS");
            // If you need a password to unlock the key store
            char[] password = "changeit".toCharArray();
            keyStore.load(new FileInputStream("/home/marko/Tesobe/GitHub/OBP-Hydra-OAuth2/src/main/resources/cert/ofpilot.jks"), password);
            // Extract keys and output into JWK set; the secord parameter allows lookup 
            // of passwords for individual private and secret keys in the store
            JWKSet jwkSet = JWKSet.load(keyStore, null);
            jwkClient = jwkSet.getKeys().get(0);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (CertificateException | KeyStoreException e) {
            e.printStackTrace();
        }
    }

}
