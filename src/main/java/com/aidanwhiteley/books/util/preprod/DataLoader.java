package com.aidanwhiteley.books.util.preprod;

import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class DataLoader {

    private static final String BOOKS_COLLECTION = "book";
    private static final String USERS_COLLECTION = "user";
    private static final Logger LOGGER = LoggerFactory.getLogger(DataLoader.class);
    private static final String SEPARATOR = "**************************************************************************";

    private final MongoTemplate template;

    @Value("${books.reload.development.data}")
    private boolean reloadDevelopmentData;

    @Autowired
    public DataLoader(MongoTemplate mongoTemplate) {
        this.template = mongoTemplate;
    }

    /**
     * Reload data for development and integration tests. Whether this runs or
     * not is also controlled by the books.reload.development.data config
     * setting.
     * <p>
     * Reads from files where each line is expected to be a valid JSON object but
     * the whole file itself isnt a valid JSON object (hence the .data extension rather than .json).
     * <p>
     * Triple "fail safe" of checking for required Spring profile being active,
     * the config switch setting and being able to find files in the /src/test/... directories.
     */
    @Bean
    @Profile({"dev", "test", "mongo-java-server"})
    public CommandLineRunner populateDummyData() {
        return args -> {

            if (reloadDevelopmentData) {

                LOGGER.warn("");
                LOGGER.warn(SEPARATOR);
                LOGGER.warn("*** WARNING!                                                             ***");
                LOGGER.warn("*** All data is deleted and dummy data reloaded when running with        ***");
                LOGGER.warn("*** either the 'dev' or 'mongo-java-server' Spring profiles.             ***");
                LOGGER.warn("*** To persist data edit the /src/main/resources/application.yml so      ***");
                LOGGER.warn("*** spring.profiles.active is other than dev, test or mongo-java-server. ***");
                LOGGER.warn(SEPARATOR);
                LOGGER.warn("");

                List<String> jsons;

                ClassPathResource classPathResource = new ClassPathResource("sample_data/books.data");
                try (InputStream resource = classPathResource.getInputStream();
                     InputStreamReader inputStreamReader = new InputStreamReader(resource, StandardCharsets.UTF_8);
                     BufferedReader bufferedReader = new BufferedReader(inputStreamReader)) {

                    // Clearing and loading data into books collection. We do this _after_ checking for the
                    // existence of the file that holds the test data - as this file should not be part of
                    // any build and deployment to a running instance.
                    LOGGER.info(SEPARATOR);
                    LOGGER.info("Clearing books collection and loading development data for books project");
                    template.dropCollection(BOOKS_COLLECTION);

                    jsons = bufferedReader.lines().collect(Collectors.toList());
                    jsons.stream().map(Document::parse).forEach(i -> template.insert(i, BOOKS_COLLECTION));
                } catch (FileNotFoundException fe) {
                    LOGGER.error(SEPARATOR);
                    LOGGER.error("*** ERROR!                                                               ***");
                    LOGGER.error("*** You are trying to drop the collections in Mongo in an environment    ***");
                    LOGGER.error("*** where test resources / files are not part of the deployed build.     ***");
                    LOGGER.error(SEPARATOR);
                    throw new IllegalStateException("Application incorrectly configured");
                }


                classPathResource = new ClassPathResource("sample_data/users.data");
                try (InputStream resource = classPathResource.getInputStream();
                     InputStreamReader inputStreamReader = new InputStreamReader(resource, StandardCharsets.UTF_8);
                     BufferedReader bufferedReader = new BufferedReader(inputStreamReader)) {

                    // Clearing and loading data into user collection - happens after index creation file found and loaded
                    LOGGER.info("Clearing users collection and loading development data for books project");
                    template.dropCollection(USERS_COLLECTION);

                    jsons = bufferedReader.lines().collect(Collectors.toList());
                }
                jsons.stream().map(Document::parse).forEach(i -> template.insert(i, USERS_COLLECTION));
                LOGGER.info(SEPARATOR);
            } else {
                LOGGER.info("Development data not reloaded due to config settings");
            }
        };
    }

    /**
     * Created indexes on collections. Does not run when mongo-java-server profile is active as
     * mongo-java-server doesnt support full text indexes that cover multiple fields.
     */
    @Bean
    @Profile({"dev", "test"})
    public CommandLineRunner createIndexed() {
        return args -> {

            if (reloadDevelopmentData) {

                List<String> jsons;

                ClassPathResource classPathResource = new ClassPathResource("indexes/books.data");
                try (InputStream resource = classPathResource.getInputStream();
                     InputStreamReader inputStreamReader = new InputStreamReader(resource, StandardCharsets.UTF_8);
                     BufferedReader bufferedReader = new BufferedReader(inputStreamReader)) {
                    jsons = bufferedReader.lines().collect(Collectors.toList());
                }

                LOGGER.info(SEPARATOR);
                LOGGER.info("Loading indexes for books project");

                jsons.stream().map(s -> new Document().append("$eval", s)).forEach(template::executeCommand);
                LOGGER.info("Created indexes for books project");

                LOGGER.info(SEPARATOR);
            } else {
                LOGGER.info("Indexes not created due to config settings");
            }
        };
    }
}
