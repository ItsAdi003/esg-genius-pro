package dev.esgenius.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(GeminiProperties.class)
public class GeminiConfig {

    public static final String CLASSIFICATION_REST_CLIENT = "geminiClassificationRestClient";

    /** Default client (assistant): short read timeout, matched to the assistant's total deadline. */
    @Bean
    @Primary
    RestClient geminiRestClient(GeminiProperties properties) {
        return buildClient(properties, properties.getReadTimeout());
    }

    /** Compliance classification: a batched response is longer, so it gets its own, longer read timeout. */
    @Bean(name = CLASSIFICATION_REST_CLIENT)
    RestClient geminiClassificationRestClient(GeminiProperties properties) {
        return buildClient(properties, properties.getClassificationReadTimeout());
    }

    private static RestClient buildClient(GeminiProperties properties, java.time.Duration readTimeout) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(properties.getConnectTimeout());
        requestFactory.setReadTimeout(readTimeout);

        return RestClient.builder()
                .baseUrl(properties.getBaseUrl())
                .requestFactory(requestFactory)
                .build();
    }
}
