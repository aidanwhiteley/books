package com.aidanwhiteley.books.repository.config;

import com.mongodb.ServerAddress;
import com.mongodb.MongoClient;
import com.mongodb.lang.NonNull;
import de.bwaldvogel.mongo.MongoServer;
import de.bwaldvogel.mongo.backend.memory.MemoryBackend;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.mongodb.config.AbstractMongoConfiguration;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

import java.net.InetSocketAddress;

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
        MongoServer server = new MongoServer(new MemoryBackend());

        // bind on a random local port
        InetSocketAddress serverAddress = server.bind();

        MongoClient client = new MongoClient(new ServerAddress(serverAddress));
        return client;
    }
}

/*
 Fongo not currently supported in the Books project - see https://github.com/aidanwhiteley/books/issues/39
 */
//public class FongoConfig {
//
//}