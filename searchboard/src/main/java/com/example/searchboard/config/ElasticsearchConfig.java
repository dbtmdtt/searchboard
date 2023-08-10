package com.example.searchboard.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class ElasticsearchConfig {
    private String elasticsearchUris;
    private final String elasticsearchClusterName;
    private final String elasticsearchUsername;
    private final String elasticsearchPassword;
    @Autowired
    public ElasticsearchConfig(
            @Value("${spring.elasticsearch.uris}") String elasticsearchUris,
            @Value("${spring.elasticsearch.rest.cluster-name}") String elasticsearchClusterName,
            @Value("${spring.elasticsearch.username}") String elasticsearchUsername,
            @Value("${spring.elasticsearch.password}") String elasticsearchPassword) {
        this.elasticsearchUris = elasticsearchUris;
        this.elasticsearchClusterName = elasticsearchClusterName;
        this.elasticsearchUsername = elasticsearchUsername;
        this.elasticsearchPassword = elasticsearchPassword;
    }

    @Bean
    public RestHighLevelClient elasticsearchClient() {
        log.debug("ssss : {}", elasticsearchUris);
        String[] uriArr = elasticsearchUris.split(",");
        HttpHost[] httpHosts = new HttpHost[uriArr.length];
        for (int i = 0; i < uriArr.length; i++) {
            httpHosts[i] = HttpHost.create(uriArr[i]);
        }

        CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(AuthScope.ANY,
                new UsernamePasswordCredentials("admin", "jys1234"));

        return new RestHighLevelClient(
                RestClient.builder(httpHosts)
                        .setHttpClientConfigCallback(httpClientBuilder ->
                                httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider)
                        )
        );
    }
}
