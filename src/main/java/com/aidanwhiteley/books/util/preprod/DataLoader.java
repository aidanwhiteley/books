package com.aidanwhiteley.books.util.preprod;

import com.aidanwhiteley.books.domain.Book;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.TextIndexDefinition;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;


@Component
public class DataLoader {

    private static final String BOOKS_COLLECTION = "book";
    private static final String USERS_COLLECTION = "user";
    private static final String BOOKS_API_SEARCH_COLLECTION = "googleBookSearch";
    private static final Logger LOGGER = LoggerFactory.getLogger(DataLoader.class);
    private static final String AUTO_LOGON_ID = "Dummy12345678";
    private static final String IN_MEMORY_MONGODB_SPRING_PROFILE = "mongo-java-server";

    private final MongoTemplate template;
    private final PreProdWarnings preProdWarnings;
    private final Environment environment;

    @Value("${books.reload.development.data}")
    private boolean reloadDevelopmentData;

    @Value("${books.autoAuthUser}")
    private boolean autoAuthUser;

    public DataLoader(MongoTemplate mongoTemplate, PreProdWarnings preProdWarnings, Environment environment) {
        this.template = mongoTemplate;
        this.preProdWarnings = preProdWarnings;
        this.environment = environment;
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
    @Profile({"dev-mongo-java-server", "dev-mongo-java-server-no-auth", "dev-mongodb-no-auth", "dev-mongodb", "ci"})
    public CommandLineRunner populateDummyData() {
        return args -> {

            if (reloadDevelopmentData) {

                preProdWarnings.displayDataReloadWarningMessage();
                loadBooksData();
                loadUserData();
                createFullTextIndex();

            } else {
                LOGGER.info("Development data not reloaded due to config settings");
            }
        };
    }

    private void loadBooksData() throws IOException {
        List<String> jsons;
        ClassPathResource classPathResource = new ClassPathResource("sample_data/books.data");
        try (InputStream resource = classPathResource.getInputStream();
             InputStreamReader inputStreamReader = new InputStreamReader(resource, StandardCharsets.UTF_8);
             BufferedReader bufferedReader = new BufferedReader(inputStreamReader)) {

            // Clearing and loading data into books collection. We do this _after_ checking for the
            // existence of the file that holds the test data.
            LOGGER.info("Clearing books collection and loading development data for books project");
            if (template.collectionExists(BOOKS_COLLECTION)) {
                template.dropCollection(BOOKS_COLLECTION);
            }

            jsons = bufferedReader.lines().toList();
            jsons.stream().map(Document::parse).forEach(i -> template.insert(i, BOOKS_COLLECTION));

            // Clearing books search collection.
            LOGGER.info("Clearing books search cache collection");
            if (template.collectionExists(BOOKS_API_SEARCH_COLLECTION)) {
                template.dropCollection(BOOKS_API_SEARCH_COLLECTION);
            }
        }
    }

    private void loadUserData() throws IOException {
        List<String> jsons;
        ClassPathResource classPathResource = new ClassPathResource("sample_data/users.data");
        try (InputStream resource = classPathResource.getInputStream();
             InputStreamReader inputStreamReader = new InputStreamReader(resource, StandardCharsets.UTF_8);
             BufferedReader bufferedReader = new BufferedReader(inputStreamReader)) {

            // Clearing and loading data into user collection - happens after user creation file found and loaded
            LOGGER.info("Clearing users collection and loading development data for books project");
            if (template.collectionExists(USERS_COLLECTION)) {
                template.dropCollection(USERS_COLLECTION);
            }

            jsons = bufferedReader.lines().toList();
        }
        jsons.stream().map(Document::parse).forEach(i -> {
            boolean autoAuthUserServiceId = i.get("authenticationServiceId").toString().contains(AUTO_LOGON_ID);
            // Only insert the user data for the "auto logon" user if the config says to
            if (!autoAuthUserServiceId || autoAuthUser) {
                template.insert(i, USERS_COLLECTION);
            }
        });
    }

    /**
     * The creation of indexes for the MongoDb is outside of the application code to allow
     * better tweaking of those indexes over time.
     * Almost all tests run fine without indexes in place - the test data isn't large.
     * The exception is the tests that require a full text index to be in place for "search" to work.
     * Therefore, we run in the full text index below when loading test data - but not
     * when the Spring profile means we are running against the in memory mongo-java-server
     * as that fale Mongo doesn't support full text indexes currently.
     *
     * The real application index creation commands are in
     * /src/main/resources/indexes/books.data
     * It does not matter much if the index below gets out of step with the real index - in terms
     * of the field weightings at least!
     */
    private void createFullTextIndex() {

        if (Arrays.stream(this.environment.getActiveProfiles()).noneMatch(s -> s.contains(IN_MEMORY_MONGODB_SPRING_PROFILE))) {
            TextIndexDefinition textIndex = new TextIndexDefinition.TextIndexDefinitionBuilder()
                    .onField("title", 10F)
                    .onField("author", 10F)
                    .onField("genre", 5F)
                    .onField("summary", 4F)
                    .onField("comments.comment", 3F)
                    .onField("googleBookDetails.volumeInfo.description", 1F)
                    .named("fullTextIndexForTests")
                    .build();
            template.indexOps(Book.class).ensureIndex(textIndex);
        }
    }

}
