package com.aidanwhiteley.books.util;

import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {"books.client.enableCORS=false"})
public abstract class IntegrationTest {

}
