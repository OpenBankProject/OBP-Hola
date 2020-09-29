package com.openbankproject.hydra.auth;

import okhttp3.Call;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.RestTemplate;
import sh.ory.hydra.ApiCallback;
import sh.ory.hydra.ApiClient;
import sh.ory.hydra.ApiException;
import sh.ory.hydra.Pair;
import sh.ory.hydra.api.PublicApi;
import sh.ory.hydra.model.WellKnown;

import java.io.IOException;
import java.time.Duration;
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
    public PublicApi hydraPublic() {
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
        return new PublicApi(apiClient);
    }

    @Bean
    public WellKnown openIDConfiguration(PublicApi hydraPublic) throws ApiException {
        return hydraPublic.discoverOpenIDConfiguration();
    }

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {

        return builder
                .setConnectTimeout(Duration.ofSeconds(60))
                .setReadTimeout(Duration.ofSeconds(30))
                .interceptors(this::headerIntercept)
                .build();
    }

    private ClientHttpResponse headerIntercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
        HttpHeaders headers = request.getHeaders();
        if(headers.getContentType() == null) {
            headers.set(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        }
        return execution.execute(request, body);
    }
}
