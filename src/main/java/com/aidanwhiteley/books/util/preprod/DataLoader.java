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
import java.io.IOException;
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
    private static final String SEPARATOR = "****************************************************************************";
    public static final String AUTO_LOGON_ID = "Dummy12345678";

    private final MongoTemplate template;

    @Value("${books.reload.development.data}")
    private boolean reloadDevelopmentData;

    @Value("${books.autoAuthUser}")
    private boolean autoAuthUser;

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
     * "Fail safe" checking for required Spring profile being active and the config switch setting.
     */
    @Bean
    @Profile({"dev", "travis", "mongo-java-server", "mongo-java-server-no-auth"})
    public CommandLineRunner populateDummyData() {
        return args -> {

            if (reloadDevelopmentData) {

                displayWarningMessage();
                loadBooksData();
                loadUserData();

                LOGGER.info(SEPARATOR);
            } else {
                LOGGER.info("Development data not reloaded due to config settings");
            }
        };
    }

    private void displayWarningMessage() {
        LOGGER.warn("");
        LOGGER.warn(SEPARATOR);
        LOGGER.warn("*** WARNING!                                                             ***");
        LOGGER.warn("*** All data is deleted and dummy data reloaded when running with        ***");
        LOGGER.warn("*** the 'dev', 'mongo-java-server' or 'mongo-java-server-no-auth' Spring ***");
        LOGGER.warn("*** profiles.                                                            ***");
        LOGGER.warn("*** To persist data edit the /src/main/resources/application.yml so      ***");
        LOGGER.warn("*** spring.profiles.active is other than the above profiles.             ***");
        LOGGER.warn(SEPARATOR);
        LOGGER.warn("");
    }


    private void loadBooksData() throws IOException {
        List<String> jsons;
        ClassPathResource classPathResource = new ClassPathResource("sample_data/books.data");
        try (InputStream resource = classPathResource.getInputStream();
             InputStreamReader inputStreamReader = new InputStreamReader(resource, StandardCharsets.UTF_8);
             BufferedReader bufferedReader = new BufferedReader(inputStreamReader)) {

            // Clearing and loading data into books collection. We do this _after_ checking for the
            // existence of the file that holds the test data.
            LOGGER.info(SEPARATOR);
            LOGGER.info("Clearing books collection and loading development data for books project");
            template.dropCollection(BOOKS_COLLECTION);

            jsons = bufferedReader.lines().collect(Collectors.toList());
            jsons.stream().map(Document::parse).forEach(i -> template.insert(i, BOOKS_COLLECTION));
        }
    }

    private void loadUserData() throws IOException {
        List<String> jsons;
        ClassPathResource classPathResource = new ClassPathResource("sample_data/users.data");
        try (InputStream resource = classPathResource.getInputStream();
             InputStreamReader inputStreamReader = new InputStreamReader(resource, StandardCharsets.UTF_8);
             BufferedReader bufferedReader = new BufferedReader(inputStreamReader)) {

            // Clearing and loading data into user collection - happens after index creation file found and loaded
            LOGGER.info("Clearing users collection and loading development data for books project");
            template.dropCollection(USERS_COLLECTION);

            jsons = bufferedReader.lines().collect(Collectors.toList());
        }
        jsons.stream().map(Document::parse).forEach(i -> {
            boolean autoAuthUserServiceId = i.get("authenticationServiceId").toString().contains(AUTO_LOGON_ID);
            // Only insert the user data for the "auto logon" user if the config says to
            if ((autoAuthUserServiceId && autoAuthUser) || !autoAuthUserServiceId) {
                template.insert(i, USERS_COLLECTION);
            }
        });
    }



    /**
     * Created indexes on collections. Does not run when mongo-java-server(-no-auth) profiles are active as
     * mongo-java-server doesnt support full text indexes that cover multiple fields.
     */
    @Bean
    @Profile({"doNotRun"})
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

                jsons.stream().map(Document::parse).forEach(template::executeCommand);
                LOGGER.info("Created indexes for books project");

                LOGGER.info(SEPARATOR);
            } else {
                LOGGER.info("Indexes not created due to config settings");
            }
        };
    }
}
