package com.aidanwhiteley.books;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

@SpringBootApplication
public class BooksApplication extends WebMvcConfigurerAdapter {

	private static final String BOOKS_COLLECTION = "book";
	private static final String USERS_COLLECTION = "user";

	private static final Logger LOGGER = LoggerFactory.getLogger(BooksApplication.class);

	@Value("${books.client.allowedCorsOrigin}")
	private String allowedCorsOrigin;

    @Value("${books.reload.development.data}")
    private boolean reloadDevelopmentData;

	@Autowired
	private MongoTemplate template;

	public static void main(String[] args) {
		SpringApplication.run(BooksApplication.class, args);
	}

	@Bean
	@Profile({ "dev"})
	public WebMvcConfigurer corsConfigurer() {
		return new WebMvcConfigurerAdapter() {
			@Override
			public void addCorsMappings(CorsRegistry registry) {
				registry.addMapping("/api/**").allowedOrigins(allowedCorsOrigin).allowedMethods("GET");
				registry.addMapping("/secure/api/**").allowedOrigins(allowedCorsOrigin).allowedMethods("GET", "POST",
						"PUT", "DELETE", "PATCH");
			}
		};
	}

    /**
     * Reload data for development and integration tests.
     * Whether this runs or not is also controlled by the
     * books.reload.development.data config setting.
     *
     * @return
     */
	@Bean
	@Profile({"dev", "integration", "unixtest"})
	public CommandLineRunner populateDummyData() {
		return args -> {

		    if (reloadDevelopmentData) {

                // Clearing and loading data into books collection
                template.dropCollection(BOOKS_COLLECTION);
                ClassPathResource classPathResource = new ClassPathResource("sample_data/books.json");
                List<String> jsons;

				try (InputStream resource = classPathResource.getInputStream()) {
					jsons =	new BufferedReader(new InputStreamReader(resource,
									StandardCharsets.UTF_8)).lines().collect(Collectors.toList());
				}
                jsons.stream().map(Document::parse).forEach(i -> template.insert(i, BOOKS_COLLECTION));
                LOGGER.info("****************************************************************************");
                LOGGER.info("Loaded development data for books as running with dev or integration profile");

                // Clearing and loading data into user collection
                template.dropCollection(USERS_COLLECTION);
                classPathResource = new ClassPathResource("sample_data/users.json");
                try (InputStream resource = classPathResource.getInputStream()) {
                    jsons =	new BufferedReader(new InputStreamReader(resource,
                            StandardCharsets.UTF_8)).lines().collect(Collectors.toList());
                }
                jsons.stream().map(Document::parse).forEach(i -> template.insert(i, USERS_COLLECTION));
                LOGGER.info("Loaded development data for users as running with dev or integration profile");
                LOGGER.info("****************************************************************************");
            } else {
		        LOGGER.info("Development data not reloaded due to config settings");
            }
		};
	}
}
