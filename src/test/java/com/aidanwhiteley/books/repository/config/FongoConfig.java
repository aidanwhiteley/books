package com.aidanwhiteley.books.repository.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@Configuration
@Profile({"fongo"})
@EnableMongoRepositories(basePackages = "com.aidanwhiteley.books")
/*
  Tests can be run against Fongo - a fake in memory replacement for
  Mongo. Means that no Mongo install is needed to run tests.
 */
//public class FongoConfig extends AbstractMongoConfiguration {
//
//    private static final String DB_NAME = "books-integration-test";
//
//    @Override
//    @NonNull
//    protected String getDatabaseName() {
//        return DB_NAME;
//    }
//
//    @Override
//    @NonNull
//    public MongoClient mongoClient() {
//        return new Fongo(getDatabaseName()).getMongo();
//    }
//}

/*
 Fongo not currently supported in the Books project - see https://github.com/aidanwhiteley/books/issues/39
 */
public class FongoConfig {

}