package com.openbankproject.hydra.auth;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.http.Header;
import org.apache.http.client.HttpClient;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.protocol.HttpContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import javax.net.ssl.*;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.SecureRandom;

@Configuration
public class RestTemplateConfig {

    @Value("${mtls.keyStore.path}")
    private Resource keyStoreResource;
    @Value("${mtls.keyStore.password}")
    private char[] keyStorePassword;
    @Value("${mtls.trustStore.path}")
    private Resource trustStoreResource;
    @Value("${mtls.trustStore.password}")
    private char[] trustStorePassword;
//    static
//    {
//        System.setProperty("javax.net.ssl.trustStore","/Users/apple/Desktop/cert/ofpilot.jks");
//        System.setProperty("javax.net.ssl.trustStorePassword", "changeit");
//        System.setProperty("javax.net.ssl.keyStore", "/Users/apple/Desktop/cert/user.jks");
//        System.setProperty("javax.net.ssl.keyStorePassword", "waiJaeP7sake");
//    }
    @Bean
    public RestTemplate restTemplate(SSLContext sslContext) {
        SSLConnectionSocketFactory socketFactory = new SSLConnectionSocketFactory(sslContext);

        HttpClient httpClient = HttpClients.custom()
                .setSSLSocketFactory(socketFactory)
                .addInterceptorLast(this::requestIntercept)
                .build();

        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory(httpClient);
        return new RestTemplate(factory);
    }

    @Bean
    public SSLContext sslContext(TrustManager[] trustManagers) throws IOException, GeneralSecurityException {
        KeyManager[] keyManagers = getKeyManagers();
        SSLContext sslContext = SSLContext.getInstance("SSL");
        sslContext.init(keyManagers, trustManagers, new SecureRandom());
        return sslContext;
    }

    private KeyManager[] getKeyManagers() throws IOException, GeneralSecurityException {
        String alg = KeyManagerFactory.getDefaultAlgorithm();
        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(alg);
        KeyStore ks = KeyStore.getInstance("jks");
        try (InputStream inputStream = keyStoreResource.getInputStream()){
            ks.load(inputStream, keyStorePassword);
        }
        keyManagerFactory.init(ks, keyStorePassword);
        return keyManagerFactory.getKeyManagers();
    }
    @Bean
    public TrustManager[] trustManagers() throws IOException, GeneralSecurityException {

        String alg = TrustManagerFactory.getDefaultAlgorithm();
        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(alg);

        KeyStore ks = KeyStore.getInstance("jks");
        try(InputStream input = trustStoreResource.getInputStream()) {
            ks.load(input, trustStorePassword);
        }
        trustManagerFactory.init(ks);
        return trustManagerFactory.getTrustManagers();
    }

    private void requestIntercept(org.apache.http.HttpRequest request, HttpContext httpContext) {
        Header[] headers = request.getHeaders(HttpHeaders.CONTENT_TYPE);
        if(ArrayUtils.isEmpty(headers)) {
            request.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        }
    }
}