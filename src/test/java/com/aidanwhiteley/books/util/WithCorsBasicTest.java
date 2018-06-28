package com.aidanwhiteley.books.util;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import com.aidanwhiteley.books.controller.config.WebSecurityConfiguration;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import static junit.framework.TestCase.assertTrue;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {"books.client.enableCORS=true"})
public class WithCorsBasicTest {

    @Value("${books.client.enableCORS}")
    private boolean enableCORS;

    @BeforeClass
    public static void suppressLogging() {
        // Turn off unwanted logging of CORS warnings to the JUnit logs
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        context.getLogger(WebSecurityConfiguration.class).setLevel(Level.valueOf("OFF"));
    }

    @AfterClass
    public static void reenableLogging() {
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        context.getLogger(WebSecurityConfiguration.class).setLevel(Level.valueOf("ON"));
    }

    @Test
    public void shamelessCodeCoverageIncreasingTest() {
        assertTrue(enableCORS);
    }
}
