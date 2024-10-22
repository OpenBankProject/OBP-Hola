package com.openbankproject.hydra.auth;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.jwk.RSAKey;
import com.openbankproject.JwsUtil;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.*;
import org.apache.http.client.HttpClient;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.BufferedHttpEntity;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.protocol.HttpContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import sun.security.provider.X509Factory;

import javax.net.ssl.*;
import javax.servlet.http.HttpSession;
import java.io.*;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPublicKey;
import java.text.ParseException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Configuration
public class RestTemplateConfig {
    private static final Logger logger = LoggerFactory.getLogger(RestTemplateConfig.class);

    @Value("${mtls.keyStore.path}")
    private Resource keyStoreResource;
    @Value("${mtls.keyStore.password}")
    private char[] keyStorePassword;
    @Value("${mtls.keyStore.alias}")
    private String keyStoreAlias;
    @Value("${mtls.trustStore.path}")
    private Resource trustStoreResource;
    @Value("${mtls.trustStore.password}")
    private char[] trustStorePassword;
    @Value("${force_jws:}")
    private String forceJws;

    @Bean
    public RestTemplate restTemplate(SSLContext sslContext) {
        SSLConnectionSocketFactory socketFactory = new SSLConnectionSocketFactory(sslContext);

        HttpClient httpClient = HttpClients.custom()
                .setSSLSocketFactory(socketFactory)
                .addInterceptorLast(this::requestIntercept)
                .addInterceptorLast(this::responseIntercept)
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
    
    public String getOrEmptyValue(String name, org.apache.http.HttpResponse response) {
        if(response.getFirstHeader(name) != null) {
            return response.getFirstHeader(name).getValue();
        } else {
            return "";
        }
    }

    private void traceResponse(HttpResponse response, String body) throws IOException {
        logger.info("=========================== response begin ================================================ Session ID : {}", getSessionId());
        logger.info("=== Status Line : {}, Session ID : {}", response.getStatusLine(), getSessionId());
        logger.info("=== Headers : {}, Session ID : {}", StringUtils.join(response.getAllHeaders(), "; "), getSessionId());
        logger.info("=== Response body: {}, Session ID : {}", body, getSessionId());
        logger.info("=========================== response end =================================================== Session ID : {}", getSessionId());
    }

    private void responseIntercept(org.apache.http.HttpResponse response, HttpContext httpContext) throws IOException {
        HttpRequest req = (HttpRequest)httpContext.getAttribute("http.request");
        String uri = req.getRequestLine().getUri();
        // Transform non-repeatable entity => repeatable entity.
        makeInputStreamOfEntityRepeatable(response);
        // Get HTTP body from the response
        String httpBody = "";
        try {
            httpBody = getHttpResponseBody(response.getEntity().getContent()).toString();
        } catch (IOException e) {
            e.printStackTrace();
        }
        traceResponse(response, httpBody);
        if(forceJws(uri)) {
            
            String xJwsSignature = getOrEmptyValue("x-jws-signature", response);
            String digest = getOrEmptyValue("digest", response);
            String verb = req.getRequestLine().getMethod().toLowerCase();
            
            String jwsProtectedHeaderAsString = "";
            String rebuiltDetachedPayload = "";
            String x5c = "";
            String sigT = "";
            try {
                // Extract JOSE Protected Header and certain values of it
                jwsProtectedHeaderAsString = JWSObject.parse(xJwsSignature).getHeader().toString();
                JsonNode jphNode = new ObjectMapper().readTree(jwsProtectedHeaderAsString);
                x5c = jphNode.get("x5c").toString().replace("[", "")
                        .replace("]", "").replace("\"", "");
                sigT = jphNode.get("sigT").toString().replace("[", "")
                        .replace("]", "").replace("\"", "");

                // Recreate detached payload
                String parsString = (String)jphNode.get("sigD").get("pars").toString();
                String[] pars = parsString.replace("[", "")
                        .replace("]", "").split(",");
                String name = "";
                for (String nameWithQuotes : Arrays.asList(pars)) {
                    name = nameWithQuotes.replace("\"", "");
                    if(name.equalsIgnoreCase("(status-line)")) {
                        rebuiltDetachedPayload = rebuiltDetachedPayload + name + ": " + verb + " " + uri + "\n";
                    } else {
                        rebuiltDetachedPayload = rebuiltDetachedPayload + name + ": " + response.getFirstHeader(name).getValue() + "\n";
                    }
                }
            } catch (ParseException | JsonProcessingException e) {
                e.printStackTrace();
            }
            String pem = X509Factory.BEGIN_CERT + x5c + X509Factory.END_CERT;
            // Verify JWS
            boolean isVerifiedJws = JwsUtil.verifyJwsSignature(sigT, httpBody, xJwsSignature, digest, pem, rebuiltDetachedPayload);
            if(!isVerifiedJws) {
                ProtocolVersion version = response.getStatusLine().getProtocolVersion();
                response.setStatusLine(version, 400, "The signed response cannot be verified.");
            }
        }
    }

    /**
     * Transform non-repeatable entity => repeatable entity.
     * Repeatable entity is the entity capable of producing its data more than once.
     * A repeatable entity's getContent() and writeTo(OutputStream) methods
     * can be called more than once whereas a non-repeatable entity's can not.
     */
    private void makeInputStreamOfEntityRepeatable(HttpResponse response) {
        final HttpEntity entity = response.getEntity();
        if (entity != null) {
            try {
                response.setEntity(new BufferedHttpEntity(entity));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private String getSessionId() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            return attributes.getRequest().getSession(false).getId();  // Get the existing session, don't create a new one
        }
        return "";
    }

    private void traceRequest(HttpRequest request, String body) throws IOException {
        logger.info("=========================== request begin ================================================ Session ID : {}", getSessionId());
        logger.info("=== Request Line : {}, Session ID : {}", request.getRequestLine(), getSessionId());
        logger.info("=== Headers : {}, Session ID : {}", StringUtils.join(request.getAllHeaders(), "; "), getSessionId());
        logger.info("=== Request body: {}, Session ID : {}", body, getSessionId());
        logger.info("============================= request end ================================================ Session ID : {}", getSessionId());
    }

    private void requestIntercept(org.apache.http.HttpRequest request, HttpContext httpContext) throws IOException {
        Header[] headers = request.getHeaders(HttpHeaders.CONTENT_TYPE);
        if(ArrayUtils.isEmpty(headers)) {
            request.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        }
        String url = request.getRequestLine().getUri();
        String httpBody = getHttpBody(request).toString();
        if(forceJws(url)) {
            String httpMethod = request.getRequestLine().getMethod().toLowerCase();
            InetAddress ip = getInetAddress();
            JwsUtil jwsUtil = new JwsUtil();
            Map<String, String> requestHeaders = new HashMap<>();
            String host = request.getFirstHeader("host").getValue().replaceAll(":8080", ":8081");
            requestHeaders.put("host", host);
            requestHeaders.put("content-type", request.getFirstHeader("content-type").getValue());
            requestHeaders.put("psu-ip-address", ip.getHostAddress());
            requestHeaders.put("psu-geo-location", "GEO:52.506931,13.144558");
            String digest = jwsUtil.createDigestHeaderValue(httpBody);
            String xJwsSignature = jwsUtil.createJwsSignature((RSAKey)getRsaKey().get("jwk"), (String)getRsaKey().get("x5c"), httpMethod, url, requestHeaders, httpBody);
            // Set request's mandatory headers
            request.setHeader("host", host);
            request.setHeader("Digest", digest);
            request.setHeader("x-jws-signature", xJwsSignature);
            request.setHeader("PSU-IP-Address", ip.getHostAddress());
            request.setHeader("PSU-GEO-Location", "GEO:52.506931,13.144558");
            request.setHeader("X-Request-ID", UUID.randomUUID().toString());
            logger.debug("Digest: " + digest);
            logger.debug("X-JWS-Signature: " + xJwsSignature);
        }
        traceRequest(request, httpBody);
    }

    private boolean forceJws(String url) {
        String[] standards  = forceJws.split(",");
        HashMap<String, String> pathOfStandard = new HashMap<String, String>();
        pathOfStandard.put("BGv1.3", "berlin-group/v1.3");
        pathOfStandard.put("OBPv4.0.0", "obp/v4.0.0");
        pathOfStandard.put("OBPv3.1.0", "obp/v3.1.0");
        pathOfStandard.put("UKv1.3", "open-banking/v3.1");
        boolean force = false;
        for (Map.Entry<String,String> path : pathOfStandard.entrySet()) {
            for (String standard : standards) {
                if(standard.equalsIgnoreCase(path.getKey())) {
                    if(url.contains(path.getValue())) force = true;
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

    public StringBuilder getHttpResponseBody(InputStream inputStream) {
        StringBuilder httpBody = new StringBuilder();
        try {
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
        return httpBody;
    }

    private HashMap<String, Object> getRsaKey() {
        KeyStore ks = null;
        String alias = keyStoreAlias;
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
        String x5cValue = "";
        if (key instanceof PrivateKey) {
            // Get certificate of public key
            X509Certificate cert = null;
            try {
                cert = (X509Certificate)ks.getCertificate(alias);
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
            try {
                Base64 encoder = new Base64(64);
                x5cValue = new String(encoder.encode(cert.getEncoded()));
            } catch (CertificateEncodingException e) {
                e.printStackTrace();
            }
        }
        HashMap<String, Object> result = new HashMap<>();
        result.put("jwk", jwk);
        result.put("x5c", x5cValue);
        return result;
    }
}