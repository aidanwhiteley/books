package com.aidanwhiteley.books.repository.config;

import com.github.fakemongo.Fongo;
import com.mongodb.MongoClient;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.mongodb.config.AbstractMongoConfiguration;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.lang.NonNull;

@Configuration
@Profile({"fongo"})
@EnableMongoRepositories(basePackages = "com.aidanwhiteley.books")
/*
  Tests can be run against Fongo - a fake in memory replacement for
  Mongo. Means that no Mongo install is needed to run tests.
 */
public class FongoConfig extends AbstractMongoConfiguration {

    private static final String DB_NAME = "books-integration-test";

    @Override
    @NonNull
    protected String getDatabaseName() {
        return DB_NAME;
    }

    @Override
    @NonNull
    public MongoClient mongoClient() {
        return new Fongo(getDatabaseName()).getMongo();
    }
}
