package com.aidanwhiteley.books.repository;

import io.netty.channel.ChannelOption;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

@Configuration
public class GoogleBooksHttpClientConfig {

    private static final String BOOKS_USER_AGENT = "Books HTTP Client";

    @Bean("googleBooksRestClient")
    public RestClient googleBooksRestClient(RestClient.Builder builder, GoogleBooksApiConfig googleBooksApiConfig) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(googleBooksApiConfig.getConnectTimeout());
        requestFactory.setReadTimeout(googleBooksApiConfig.getReadTimeout());

        return builder
                .requestFactory(requestFactory)
                .defaultHeader(HttpHeaders.USER_AGENT, BOOKS_USER_AGENT)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Bean("googleBooksWebClient")
    public WebClient googleBooksWebClient(GoogleBooksApiConfig googleBooksApiConfig) {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, googleBooksApiConfig.getConnectTimeout())
                .responseTimeout(Duration.ofMillis(googleBooksApiConfig.getReadTimeout()));

        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .defaultHeader(HttpHeaders.USER_AGENT, BOOKS_USER_AGENT)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }
}
