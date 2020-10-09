package com.openbankproject.hydra.auth;

import okhttp3.Call;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import sh.ory.hydra.ApiCallback;
import sh.ory.hydra.ApiClient;
import sh.ory.hydra.ApiException;
import sh.ory.hydra.Pair;
import sh.ory.hydra.api.PublicApi;
import sh.ory.hydra.model.WellKnown;

import javax.net.ssl.SSLContext;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;

@SpringBootApplication
public class ObpHydraAuthApplication {
    @Value("${oauth2.public_url}")
    private String hydraPublicUrl;

    @Value("${oauth2.client_id}")
    private String hydraClientId;
    @Value("${oauth2.client_secret}")
    private String hydraClientSecret;

    public static void main(String[] args) {
        SpringApplication.run(ObpHydraAuthApplication.class, args);
    }

    @Bean
    public PublicApi hydraPublic(SSLContext sslContext) {
        // hydra client have this setting "token_endpoint_auth_method": "client_secret_post"
        // the formParams must contains client_id and client_secret parameters
        ApiClient apiClient = new ApiClient(){
            public Call buildCall(String path, String method, List<Pair> queryParams, List<Pair> collectionQueryParams, Object body, Map<String, String> headerParams, Map<String, String> cookieParams, Map<String, Object> formParams, String[] authNames, ApiCallback callback) throws ApiException {
                formParams.put("client_id", hydraClientId);
                formParams.put("client_secret", hydraClientSecret);
                return super.buildCall( path,  method, queryParams, collectionQueryParams, body, headerParams, cookieParams, formParams, authNames, callback);
            }
        };
        apiClient.setBasePath(hydraPublicUrl);
        // config MTLS for hydra client
        final OkHttpClient httpClient = apiClient.getHttpClient();
        final OkHttpClient okHttpClient = httpClient.newBuilder().sslSocketFactory(sslContext.getSocketFactory()).build();
        apiClient.setHttpClient(okHttpClient);
        return new PublicApi(apiClient);
    }

    @Bean
    public WellKnown openIDConfiguration(PublicApi hydraPublic) throws ApiException {
        return hydraPublic.discoverOpenIDConfiguration();
    }
}
