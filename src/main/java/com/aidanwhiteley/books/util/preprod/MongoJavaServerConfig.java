package com.aidanwhiteley.books.util.preprod;

import com.mongodb.ServerAddress;
import com.mongodb.MongoClient;
import com.mongodb.lang.NonNull;
import de.bwaldvogel.mongo.MongoServer;
import de.bwaldvogel.mongo.backend.memory.MemoryBackend;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.mongodb.config.AbstractMongoConfiguration;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

import java.net.InetSocketAddress;

@Configuration
@Profile({"mongo-java-server"})
@EnableMongoRepositories(basePackages = "com.aidanwhiteley.books")
/*
  Tests can be run against mongo-java-server - a fake in memory replacement for
  Mongo. Means that no Mongo install is needed to run tests.
 */
public class MongoJavaServerConfig extends AbstractMongoConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(MongoJavaServerConfig.class);
    private static final String DB_NAME = "books-integration-test";

    @Override
    @NonNull
    protected String getDatabaseName() {
        return DB_NAME;
    }

    @Override
    @NonNull
    public MongoClient mongoClient() {
        LOGGER.warn("");
        LOGGER.warn("****************************************************************************");
        LOGGER.warn("*** WARNING!                                                             ***");
        LOGGER.warn("*** You are running with an in memory version of Mongo.                  ***");
        LOGGER.warn("*** All data is lost when the application ends.                          ***");
        LOGGER.warn("*** To use a real MongoDb edit the /src/main/resources/application.yml   ***");
        LOGGER.warn("*** so spring.profiles.active is other than mongo-java-server.           ***");
        LOGGER.warn("****************************************************************************");
        LOGGER.warn("");

        MongoServer server = new MongoServer(new MemoryBackend());

        // bind on a random local port
        InetSocketAddress serverAddress = server.bind();

        return new MongoClient(new ServerAddress(serverAddress));
    }
}