package com.openban.hydra.auth;

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
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;
import sh.ory.hydra.ApiClient;
import sh.ory.hydra.Configuration;
import sh.ory.hydra.api.AdminApi;
import sh.ory.hydra.api.PublicApi;

import java.io.IOException;
import java.time.Duration;

@SpringBootApplication
public class ObpHydraAuthApplication {
    @Value("${oauth2.admin_url}")
    private String hydraAdminUrl;
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
    public AdminApi hydraAdmin() {
        ApiClient apiClient = new ApiClient();
        apiClient.setBasePath(hydraAdminUrl);
        return new AdminApi(apiClient);
    }

    @Bean
    public PublicApi hydraPublic() {
        ApiClient apiClient = new ApiClient();
        apiClient.setBasePath(hydraClientSecret);
        return new PublicApi(apiClient);
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
