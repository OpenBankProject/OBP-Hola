package com.openbankproject.hydra.auth;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.*;
import java.security.cert.CertificateException;
import java.text.ParseException;

@Configuration
public class ClientConfig {
    Logger log = LoggerFactory.getLogger(ClientConfig.class);

    private JWK jwkClient;

    public JWK getJwkClient() {
        return jwkClient;
    }

    @PostConstruct
    private void initiate() throws ParseException, JOSEException {
/*        // Load the key store from file
        try {
            // Specify the key store type, e.g. JKS
            KeyStore keyStore = KeyStore.getInstance("JKS");
            // If you need a password to unlock the key store
            char[] password = "waiJaeP7sake".toCharArray();
            keyStore.load(new FileInputStream("/home/marko/Tesobe/GitHub/OBP-Hydra-OAuth2/src/main/resources/cert/user.jks"), password);
            // Extract keys and output into JWK set; the secord parameter allows lookup 
            // of passwords for individual private and secret keys in the store
            JWKSet jwkSet = JWKSet.load(keyStore, null);


            String alias = "myalias";

            Key key = null;
            try {
                key = keyStore.getKey(alias, "waiJaeP7sake".toCharArray());
            } catch (UnrecoverableKeyException e) {
                e.printStackTrace();
            }
            if (key instanceof PrivateKey) {
                key = (PrivateKey) key;
            }
            
            
            jwkClient = jwkSet.getKeys().get(0);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (CertificateException | KeyStoreException e) {
            e.printStackTrace();
        }*/
    }

}
