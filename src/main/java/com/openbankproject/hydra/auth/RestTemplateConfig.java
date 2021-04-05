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

    @javax.annotation.Resource
    private ClientConfig clientConfig;
    @javax.annotation.Resource
    private HydraConfig hydraConfig;

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
        String url = request.getRequestLine().getUri();
        String httpMethod = request.getRequestLine().getMethod().toLowerCase();
        String httpBody = getHttpBody(request).toString();
        InetAddress ip = getInetAddress();
        JwsUtil jwsUtil = new JwsUtil();
        Map<String, String> requestHeaders = new HashMap<>();
        requestHeaders.put("host", request.getFirstHeader("host").getValue());
        requestHeaders.put("content-type", request.getFirstHeader("content-type").getValue());
        requestHeaders.put("psu-ip-address", ip.getHostAddress());
        request.setHeader("psu-ip-address", ip.getHostAddress());
        requestHeaders.put("psu-geo-location", "GEO:52.506931,13.144558");
        request.setHeader("psu-geo-location", "GEO:52.506931,13.144558");
        String digest = jwsUtil.createDigestHeaderValue(httpBody.toString());
        String xJwsSignature = null;
        request.setHeader("digest", digest);
        xJwsSignature = JwsUtil.createJwsSignature(getRsaKey(), httpMethod, url, requestHeaders, httpBody.toString());
        request.setHeader("x-jws-signature", xJwsSignature);
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