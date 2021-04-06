package com.openbankproject.hydra.auth;

import com.nimbusds.jose.jwk.RSAKey;
import com.openbankproject.JwsUtil;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpRequest;
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
import java.io.*;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.interfaces.RSAPublicKey;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

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
    @Value("${force_jws}")
    private String forceJws;

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
        if(forceJws()) {
            String url = request.getRequestLine().getUri();
            String httpMethod = request.getRequestLine().getMethod().toLowerCase();
            String httpBody = getHttpBody(request).toString();
            InetAddress ip = getInetAddress();
            JwsUtil jwsUtil = new JwsUtil();
            Map<String, String> requestHeaders = new HashMap<>();
            requestHeaders.put("host", request.getFirstHeader("host").getValue());
            requestHeaders.put("content-type", request.getFirstHeader("content-type").getValue());
            requestHeaders.put("psu-ip-address", ip.getHostAddress());
            requestHeaders.put("psu-geo-location", "GEO:52.506931,13.144558");
            String digest = jwsUtil.createDigestHeaderValue(httpBody);
            String xJwsSignature = jwsUtil.createJwsSignature(getRsaKey(), httpMethod, url, requestHeaders, httpBody);
            // Set request's mandatory headers
            request.setHeader("Digest", digest);
            request.setHeader("x-jws-signature", xJwsSignature);
            request.setHeader("PSU-IP-Address", ip.getHostAddress());
            request.setHeader("PSU-GEO-Location", "GEO:52.506931,13.144558");
            request.setHeader("X-Request-ID", UUID.randomUUID().toString());
        }
    }

    private boolean forceJws() {
        String[] standards  = forceJws.split(",");
        HashMap<String, String> pathOfStandard = new HashMap<String, String>();
        pathOfStandard.put("BGv1.3", "berlin-group/v1.3");
        pathOfStandard.put("OBPv4.0.0", "obp/v4.0.0");
        pathOfStandard.put("OBPv3.1.0", "obp/v3.1.0");
        pathOfStandard.put("UKv1.3", "open-banking/v3.1");
        boolean force = false;
        for (String i : pathOfStandard.keySet()) {
            for (String standard : standards) {
                if(standard.equalsIgnoreCase(i)) {
                    force = true;
                }
            }
        }
        return force;
    }

    private InetAddress getInetAddress() {
        InetAddress ip = null;
        try {
            ip = InetAddress.getLocalHost();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        return ip;
    }

    private StringBuilder getHttpBody(HttpRequest request) {
        StringBuilder httpBody = new StringBuilder();
        if(request instanceof HttpEntityEnclosingRequest) {
            HttpEntityEnclosingRequest enclosingRequest = ((HttpEntityEnclosingRequest) request);
            HttpEntity requestEntity = enclosingRequest.getEntity();

            try {
                InputStream inputStream =  requestEntity.getContent();
                try (Reader reader = new BufferedReader(new InputStreamReader
                        (inputStream, Charset.forName(StandardCharsets.UTF_8.name())))) {
                    int c = 0;
                    while ((c = reader.read()) != -1) {
                        httpBody.append((char) c);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return httpBody;
    }

    private RSAKey getRsaKey() {
        KeyStore ks = null;
        String alias = "1";
        Key key = null;
        try {
            ks = KeyStore.getInstance("jks");
            InputStream inputStream = keyStoreResource.getInputStream();
            ks.load(inputStream, keyStorePassword);
            key = ks.getKey(alias, keyStorePassword);
        } catch (KeyStoreException | IOException | NoSuchAlgorithmException | UnrecoverableKeyException | CertificateException e) {
            e.printStackTrace();
        }
        KeyPair keyPair = null;
        RSAKey jwk = null;
        if (key instanceof PrivateKey) {
            // Get certificate of public key
            Certificate cert = null;
            try {
                cert = ks.getCertificate(alias);
            } catch (KeyStoreException e) {
                e.printStackTrace();
            }

            // Get public key
            PublicKey publicKey = cert.getPublicKey();

            // Return a key pair
            keyPair = new KeyPair(publicKey, (PrivateKey) key);
            // Convert to JWK format
            jwk = new RSAKey.Builder(
                    (RSAPublicKey) keyPair.getPublic())
                    .privateKey(keyPair.getPrivate())
                    .build();
        }
        return jwk;
    }
}