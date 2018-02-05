package com.aidanwhiteley.books.repository.config;

import com.github.fakemongo.Fongo;
import com.mongodb.Mongo;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.AbstractMongoConfiguration;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.test.context.ActiveProfiles;

@Configuration
@EnableMongoRepositories(basePackages = "com.aidanwhiteley.books")
@ActiveProfiles("integration")
/**
 * Integration tests are run against Fongo - a fake in memory replacement for
 * Mongo. Means that no Mongo install is needed to run tests.
 */
public class FongoConfig extends AbstractMongoConfiguration {

    private static final String DB_NAME = "books-integration-test";

    @Override
    protected String getDatabaseName() {
        return DB_NAME;
    }

    @Override
    @Bean
    public Mongo mongo() {
        return new Fongo(getDatabaseName()).getMongo();
    }

}
