package com.aidanwhiteley.books;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
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

	public static final String BOOKS_COLLECTION = "book";
	public static final String USERS_COLLECTION = "user";

	private static final Logger LOGGER = LoggerFactory.getLogger(BooksApplication.class);

	@Value("${books.client.allowedCorsOrigin}")
	private String allowedCorsOrigin;

	@Autowired
	private MongoTemplate template;

	public static void main(String[] args) {
		SpringApplication.run(BooksApplication.class, args);
	}

	@Bean
	@Profile({ "dev", "test" })
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

	@Bean
	@Profile("dev")
	public CommandLineRunner populateDummyData() {
		return args -> {

			// Clearing and loading data into books collection
			template.dropCollection(BOOKS_COLLECTION);
			ClassPathResource classPathResource = new ClassPathResource("sample_data/books.json");
			List<String> jsons = new ArrayList<>();
			Stream<String> stream = Files.lines(Paths.get(classPathResource.getFile().toURI()), StandardCharsets.UTF_8);
			stream.forEach(jsons::add);
			jsons.stream().map(Document::parse).forEach(i -> template.insert(i, BOOKS_COLLECTION));
			stream.close();
			LOGGER.info("Loaded development data for books as running in dev mode");

			// Clearing and loading data into user collection
			template.dropCollection(USERS_COLLECTION);
			classPathResource = new ClassPathResource("sample_data/users.json");
			jsons = new ArrayList<>();
			stream = Files.lines(Paths.get(classPathResource.getFile().toURI()), StandardCharsets.UTF_8);
			stream.forEach(jsons::add);
			jsons.stream().map(Document::parse).forEach(i -> template.insert(i, USERS_COLLECTION));
			stream.close();
			LOGGER.info("Loaded development data for users as running in dev mode");
		};
	}
}
